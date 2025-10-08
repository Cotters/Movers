package com.jcotters.auth.domain

import javax.inject.Inject

class GetSessionUseCase @Inject constructor(
  userRepository: IUserRepository,
) {
  val session = userRepository.userSession
}