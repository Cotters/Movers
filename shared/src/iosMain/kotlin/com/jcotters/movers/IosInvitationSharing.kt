@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.jcotters.movers

import com.jcotters.contract.watchlists.domain.InvitationSharing
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

internal class IosInvitationSharing : InvitationSharing {
    override fun share(url: String) {
        var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        while (presenter.presentedViewController != null) presenter = presenter.presentedViewController!!
        val sheet = UIActivityViewController(activityItems = listOf(url),applicationActivities = null)
        sheet.popoverPresentationController?.let {
            it.sourceView = presenter.view
            it.sourceRect = CGRectMake(0.0,0.0,1.0,1.0)
        }
        presenter.presentViewController(sheet,animated = true,completion = null)
    }
}
