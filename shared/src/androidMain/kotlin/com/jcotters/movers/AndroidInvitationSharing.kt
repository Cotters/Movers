package com.jcotters.movers

import android.content.Context
import android.content.Intent
import com.jcotters.contract.watchlists.domain.InvitationSharing

internal class AndroidInvitationSharing(private val context: Context) : InvitationSharing {
    override fun share(url: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,url)
        context.startActivity(Intent.createChooser(send,"Invite to watchlist").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
