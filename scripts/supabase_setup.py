"""Use an environment token or hidden prompt; HTTP credentials travel through stdin."""

import argparse
import getpass
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import warnings

ROOT = Path(__file__).resolve().parent.parent


class SetupError(Exception):
    pass


def read_values(path, names, xcconfig=False):
    if not path.is_file():
        raise SetupError(f"Missing {path.relative_to(ROOT)}. Follow supabase/README.md.")
    values = {}
    try:
        contents = path.read_text()
    except (OSError, UnicodeError):
        raise SetupError(f"Cannot read {path.relative_to(ROOT)} as UTF-8. Check its permissions and encoding.") from None
    for line in contents.splitlines():
        match = re.match(r"\s*([\w.]+)\s*=\s*(.*?)\s*$", line)
        if not match or match[1] not in names:
            continue
        if match[1] in values:
            raise SetupError(f"Duplicate {match[1]} in {path.relative_to(ROOT)}.")
        value = match[2]
        if xcconfig:
            value = value.split("//", 1)[0].strip().replace("$()", "")
        else:
            value = value.replace(r"\:", ":").replace(r"\=", "=")
        values[match[1]] = value.strip('"')
    if set(values) != set(names) or not all(values.values()):
        raise SetupError(f"Missing backend values in {path.relative_to(ROOT)}.")
    return [values[name] for name in names]


def configuration():
    android_url, android_key = read_values(ROOT / "local.properties", ["supabase.url", "supabase.publishableKey"])
    ios_url, ios_key = read_values(ROOT / "iosApp/Local.xcconfig", ["SUPABASE_URL", "SUPABASE_PUBLISHABLE_KEY"], xcconfig=True)
    if android_url.rstrip("/") != ios_url.rstrip("/") or android_key != ios_key:
        raise SetupError("Android/iOS backend values do not match. Check the xcconfig URL escaping.")
    try:
        url = urllib.parse.urlsplit(android_url)
        port = url.port
    except ValueError:
        raise SetupError("Invalid backend URL. Check both app configs.") from None
    match = re.fullmatch(r"([a-z0-9]{20})\.supabase\.co", url.hostname or "")
    if not match or url.scheme != "https" or url.path not in ("", "/") or url.query or url.fragment or url.username or port:
        raise SetupError("Use the project's standard HTTPS Supabase URL in both app configs.")
    if not android_key.startswith("sb_publishable_") or any(char.isspace() for char in android_key):
        raise SetupError("Use a publishable key, not a personal access token or privileged backend key.")
    return match[1], android_key


def request_json(url, headers, label, payload=None):
    # macOS curl uses system trust; python.org installations may have no CA bundle.
    curl = "/usr/bin/curl" if sys.platform == "darwin" else shutil.which("curl")
    if not curl:
        raise SetupError("Install curl before running remote setup.")
    method = "PATCH" if payload is not None else "GET"
    config = [f"url = {json.dumps(url)}", f'request = "{method}"']
    config.extend(f"header = {json.dumps(name + ': ' + value)}" for name, value in headers.items())
    if payload is not None:
        config.append(f"data = {json.dumps(json.dumps(payload))}")
    try:
        response = subprocess.run(
            [curl, "--disable", "--silent", "--show-error", "--proto", "=https",
             "--connect-timeout", "10", "--max-time", "30", "--write-out", "\n%{http_code}", "--config", "-"],
            input="\n".join(config) + "\n", capture_output=True, text=True, timeout=35,
        )
    except (OSError, subprocess.TimeoutExpired):
        raise SetupError(f"{label} could not run curl or timed out. Check curl and your network connection.") from None
    if response.returncode:
        reason = {
            5: "Proxy hostname could not be resolved.",
            6: "Hostname could not be resolved. Check DNS/VPN/network access.",
            7: "Connection failed. Check network/firewall access and project availability.",
            28: "Request timed out. Check network access and retry.",
            35: "TLS handshake failed. Check your system curl and network proxy.",
            60: "Certificate verification failed. Check your system trust store or HTTPS proxy.",
            77: "The configured CA certificate file cannot be read. Check curl's certificate environment settings.",
        }.get(response.returncode, f"curl failed with exit code {response.returncode}.")
        # Headers go through stdin; raw curl output is never logged because it can contain private settings.
        raise SetupError(f"{label}: {reason}")
    body, separator, status = response.stdout.rpartition("\n")
    if not separator or not re.fullmatch(r"\d{3}", status):
        raise SetupError(f"{label} returned an invalid HTTP status.")
    if not 200 <= int(status) < 300:
        raise SetupError(f"{label} failed (HTTP {status}). Check the account/project permissions and configured keys.")
    if payload is not None:
        return {}
    try:
        data = json.loads(body)
    except ValueError:
        raise SetupError(f"{label} returned invalid JSON; response contents were not printed.") from None
    if not isinstance(data, dict):
        raise SetupError(f"Unexpected {label} response; no settings or credentials were printed.")
    return data


def auth_config(ref, token, enable=False):
    return request_json(
        f"https://api.supabase.com/v1/projects/{ref}/config/auth",
        {"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
        "Auth settings request", {"external_anonymous_users_enabled": True} if enable else None,
    )


def check_client_key(ref, key):
    request_json(f"https://{ref}.supabase.co/auth/v1/settings", {"apikey": key}, "App publishable-key check")
    print("App publishable key accepted by the configured project.", flush=True)


def run_cli(workdir, *arguments):
    # The explicit workdir and inherited token prevent using a different checkout/account's link.
    subprocess.run(["supabase", "--workdir", str(workdir), *arguments], check=True)


def access_token():
    token = os.environ.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        if not sys.stdin.isatty():
            raise SetupError("No access token. Run in an interactive terminal for a hidden prompt, or set SUPABASE_ACCESS_TOKEN for automation.")
        try:
            with warnings.catch_warnings():
                # Never let getpass fall back to echoing a credential on stdin.
                warnings.simplefilter("error", getpass.GetPassWarning)
                token = getpass.getpass("Movers Supabase access token (hidden): ")
        except (getpass.GetPassWarning, EOFError, OSError):
            raise SetupError("Cannot read the token securely. Use an interactive terminal or set SUPABASE_ACCESS_TOKEN.") from None
    if not token:
        raise SetupError("No access token entered. No remote changes were made.")
    if token != token.strip() or any(char.isspace() for char in token):
        raise SetupError("The access token contains whitespace. Enter it again without spaces or newlines.")
    # Only this process and its CLI children receive it; no login or disk storage.
    os.environ["SUPABASE_ACCESS_TOKEN"] = token
    return token


def main():
    parser = argparse.ArgumentParser(description="Movers backend setup. Uses SUPABASE_ACCESS_TOKEN or prompts securely when missing.")
    parser.add_argument("command", nargs="?", default="inspect", choices=["check", "inspect", "deploy", "verify"])
    command = parser.parse_args().command
    ref, key = configuration()
    print("Android/iOS configs match; URL and publishable-key formats are valid.", flush=True)
    if command == "check":
        return
    if not shutil.which("supabase"):
        raise SetupError("Install the Supabase CLI before running remote setup.")
    migrations = sorted((ROOT / "supabase/migrations").glob("*.sql"))
    if not migrations:
        raise SetupError("No versioned migrations found; refusing remote setup.")
    token = access_token()
    # GET validates token access to precisely the project selected by both app configs.
    settings = auth_config(ref, token)
    check_client_key(ref, key)
    print(f"Target project: {ref}", flush=True)
    print("Anonymous Sign-Ins:", "enabled" if settings.get("external_anonymous_users_enabled") else "disabled", flush=True)
    if command == "deploy" and settings.get("disable_signup"):
        raise SetupError("New user signup is disabled. Enable it in the project's Auth settings before deploying.")
    with tempfile.TemporaryDirectory(prefix="movers-supabase-") as directory:
        workdir = Path(directory)
        backend = workdir / "supabase"
        backend.mkdir()
        shutil.copy2(ROOT / "supabase/config.toml", backend / "config.toml")
        shutil.copytree(ROOT / "supabase/migrations", backend / "migrations")
        print("Linking an isolated CLI workdir. If asked for the database password, enter it locally (or leave blank to skip validation).", flush=True)
        run_cli(workdir, "link", "--project-ref", ref)
        linked = backend / ".temp/project-ref"
        if not linked.is_file() or linked.read_text().strip() != ref:
            raise SetupError("CLI link does not match the app configuration. No migration was applied.")
        if command == "inspect":
            run_cli(workdir, "migration", "list", "--linked")
            run_cli(workdir, "db", "query", "--linked", "--file", str(ROOT / "supabase/inspect.sql"))
            run_cli(workdir, "db", "push", "--linked", "--dry-run")
            print("Inspection complete. No migrations or Auth settings changed. Run deploy when ready.")
        elif command == "deploy":
            run_cli(workdir, "migration", "list", "--linked")
            run_cli(workdir, "db", "push", "--linked", "--dry-run")
            run_cli(workdir, "db", "push", "--linked")
            auth_config(ref, token, enable=True)
            verify(workdir, ref, token)
        else:
            verify(workdir, ref, token)


def verify(workdir, ref, token):
    run_cli(workdir, "db", "query", "--linked", "--file", str(ROOT / "supabase/verify.sql"))
    settings = auth_config(ref, token)
    if not settings.get("external_anonymous_users_enabled") or settings.get("disable_signup"):
        raise SetupError("Schema checks passed, but Anonymous Sign-Ins/new user signup are not both enabled.")
    run_cli(workdir, "db", "advisors", "--linked", "--type", "security", "--fail-on", "error")
    print("Schema/access checks passed; anonymous signup is enabled. Review advisor warnings and rebuild both apps for physical-device acceptance.")


if __name__ == "__main__":
    try:
        main()
    except SetupError as error:
        print(f"Backend setup: {error}", file=sys.stderr)
        sys.exit(2)
    except OSError:
        print("Backend setup: A local file or command could not be accessed. Check permissions and installed tools; no private paths were printed.", file=sys.stderr)
        sys.exit(2)
    except subprocess.CalledProcessError as error:
        print("Supabase CLI command failed. Review its output above; later steps were not run. Re-running deploy reconciles migration history.", file=sys.stderr)
        sys.exit(error.returncode if error.returncode > 0 else 1)
    except KeyboardInterrupt:
        sys.exit(130)
