@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.jcotters.backend.supabase

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

fun createIosBackend(url: String, key: String): SupabaseBackend = SupabaseBackend(url, key, KeychainCredentials())

private class KeychainCredentials : SecureCredentialStore {
    private fun setObject(dictionary: CFMutableDictionaryRef, key: CFStringRef?, value: Any) {
        val retained = CFBridgingRetain(value)
        try {
            CFDictionarySetValue(dictionary, key, retained)
        } finally {
            CFRelease(retained)
        }
    }

    private fun query(account: String): CFMutableDictionaryRef {
        val dictionary =
            CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)!!
        CFDictionarySetValue(dictionary, kSecClass, kSecClassGenericPassword)
        setObject(dictionary, kSecAttrService, "com.jcotters.movers.sharing")
        setObject(dictionary, kSecAttrAccount, account)
        return dictionary
    }

    private fun read(account: String): String? = memScoped {
        val query = query(account)
        try {
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status == errSecItemNotFound) return@memScoped null
            check(status == errSecSuccess) { "Keychain is unavailable." }
            val data = CFBridgingRelease(result.value) as NSData
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        } finally {
            CFRelease(query)
        }
    }

    private fun write(account: String, value: String) {
        val query = query(account)
        val attributes =
            CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)!!
        try {
            val data = value.encodeToByteArray().let { bytes ->
                bytes.usePinned {
                    NSData.create(
                        bytes = it.addressOf(0),
                        length = bytes.size.toULong()
                    )
                }
            }
            setObject(attributes, kSecValueData, data)
            CFDictionarySetValue(attributes, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            val status = SecItemUpdate(query, attributes)
            if (status == errSecItemNotFound) {
                setObject(query, kSecValueData, data)
                CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
                check(SecItemAdd(query, null) == errSecSuccess) { "Unable to save sharing identity." }
            } else check(status == errSecSuccess) { "Unable to save sharing identity." }
        } finally {
            CFRelease(query); CFRelease(attributes)
        }
    }

    override suspend fun read(): String? {
        val value = read("session")
        check(value != null || read("claimed") == null) { "Saved sharing identity is missing." }
        return value
    }

    override suspend fun write(value: String) {
        write("session", value); write("claimed", "true")
    }

    override suspend fun clear() {
        val query = query("session")
        try {
            val status = SecItemDelete(query); check(status == errSecSuccess || status == errSecItemNotFound)
        } finally {
            CFRelease(query)
        }
    }
}
