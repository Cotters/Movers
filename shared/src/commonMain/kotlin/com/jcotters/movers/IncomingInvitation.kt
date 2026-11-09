package com.jcotters.movers

fun receiveInvitation(url: String): Boolean = com.jcotters.contract.watchlists.domain.InvitationInbox.offer(url)
