# App icon

`movers-icon.jpeg` is the supplied source artwork for both platforms. Run `./scripts/generate-app-icons.sh` on macOS after replacing it with another square, opaque 1024×1024 image. Commit the generated Android artwork PNG and iOS AppIcon PNG with the source.

Android uses adaptive icons on every supported version (minimum API 28). The supplied flattened artwork sits in the background with a one-sixth inset to fit the visible viewport; a gradient fills the motion area and the foreground is transparent. Its themed shutter silhouette derives from Google's [Material camera icon](https://github.com/google/material-design-icons/blob/master/src/image/camera/materialicons/24px.svg), scaled into the safe zone, under [Apache 2.0](material-icons-LICENSE.txt). Update that vector separately if the logo changes.

iOS uses the opaque 1024×1024 asset and generates device sizes during compilation. Platform launchers apply their own masks; do not bake rounded corners into the source.
