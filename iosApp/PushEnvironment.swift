import Foundation
import MachO

/// Reads the running app's signed entitlement, including after distribution re-signing.
/// SecTaskCopyValueForEntitlement is not part of the public iPhone SDK.
enum PushEnvironment {
    static func current() -> String? {
        guard let url = Bundle.main.executableURL,
            let data = try? Data(contentsOf: url, options: .mappedIfSafe),
            let environment = entitlement(in: data)
        else { return nil }
        switch environment {
        case "development": return "sandbox"
        case "production": return "production"
        default: return nil
        }
    }

    static func entitlement(in data: Data) -> String? {
        // The physical iOS target builds a thin, little-endian arm64 Mach-O executable.
        guard data.uint32(at: 0, bigEndian: false) == MH_MAGIC_64,
            let count = data.uint32(at: 16, bigEndian: false),
            let commandSize = data.uint32(at: 20, bigEndian: false),
            32 + Int(commandSize) <= data.count,
            count <= commandSize / 8
        else { return nil }
        var offset = 32
        for _ in 0..<count {
            guard let command = data.uint32(at: offset, bigEndian: false),
                let size = data.uint32(at: offset + 4, bigEndian: false), size >= 8,
                offset + Int(size) <= 32 + Int(commandSize)
            else { return nil }
            if command == LC_CODE_SIGNATURE {
                guard size >= 16, let start = data.uint32(at: offset + 8, bigEndian: false),
                    let length = data.uint32(at: offset + 12, bigEndian: false),
                    Int(start) + Int(length) <= data.count
                else { return nil }
                return signedEntitlement(in: data.subdata(in: Int(start)..<Int(start) + Int(length)))
            }
            offset += Int(size)
        }
        return nil
    }

    private static func signedEntitlement(in signature: Data) -> String? {
        // Apple code-signing SuperBlob and entitlement slot formats: xnu/osfmk/kern/cs_blobs.h.
        guard signature.uint32(at: 0) == 0xfade_0cc0,
            let length = signature.uint32(at: 4), Int(length) <= signature.count,
            let count = signature.uint32(at: 8), length >= 12, count <= (length - 12) / 8
        else { return nil }
        for index in 0..<Int(count) {
            guard let slot = signature.uint32(at: 12 + index * 8),
                let offset = signature.uint32(at: 16 + index * 8),
                let blobLength = signature.uint32(at: Int(offset) + 4), blobLength >= 8,
                Int(offset) + Int(blobLength) <= Int(length)
            else { return nil }
            let contents = signature.subdata(in: Int(offset) + 8..<Int(offset) + Int(blobLength))
            if slot == 5, signature.uint32(at: Int(offset)) == 0xfade_7171,
                let plist = try? PropertyListSerialization.propertyList(from: contents, format: nil) as? [String: Any],
                let value = plist["aps-environment"] as? String
            {
                return value
            }
            if slot == 7, signature.uint32(at: Int(offset)) == 0xfade_7172,
                let value = derEnvironment(in: contents, depth: 0)
            {
                return value
            }
        }
        return nil
    }

    private static func derEnvironment(in data: Data, depth: Int) -> String? {
        guard depth < 16 else { return nil }
        var offset = 0
        while offset < data.count {
            guard let element = derElement(in: data, at: offset) else { return nil }
            if element.tag & 0x20 != 0 {
                let contents = data.subdata(in: element.range)
                if let key = derElement(in: contents, at: 0), key.tag == 0x0c,
                    String(data: contents.subdata(in: key.range), encoding: .utf8) == "aps-environment",
                    let value = derElement(in: contents, at: key.range.upperBound), value.tag == 0x0c,
                    value.range.upperBound == contents.count
                {
                    return String(data: contents.subdata(in: value.range), encoding: .utf8)
                }
                if let value = derEnvironment(in: contents, depth: depth + 1) { return value }
            }
            offset = element.range.upperBound
        }
        return nil
    }

    private static func derElement(in data: Data, at offset: Int) -> (tag: UInt8, range: Range<Int>)? {
        guard offset >= 0, offset + 2 <= data.count else { return nil }
        let tag = data[offset]
        let first = data[offset + 1]
        var start = offset + 2
        var length = Int(first)
        if first & 0x80 != 0 {
            let bytes = Int(first & 0x7f)
            guard bytes > 0, bytes <= 4, start + bytes <= data.count else { return nil }
            length = 0
            for index in start..<start + bytes { length = (length << 8) | Int(data[index]) }
            start += bytes
        }
        guard length <= data.count - start else { return nil }
        return (tag, start..<start + length)
    }
}

extension Data {
    fileprivate func uint32(at offset: Int, bigEndian: Bool = true) -> UInt32? {
        guard offset >= 0, offset + 4 <= count else { return nil }
        let bytes = self[offset..<offset + 4]
        return (bigEndian ? Array(bytes) : Array(bytes.reversed())).reduce(0) { ($0 << 8) | UInt32($1) }
    }
}
