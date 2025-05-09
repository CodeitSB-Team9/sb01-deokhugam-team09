package com.codeit.sb01_deokhugam.domain.user.service;

import com.codeit.sb01_deokhugam.domain.user.dto.request.GetPowerUsersRequest;
import com.codeit.sb01_deokhugam.domain.user.dto.response.PowerUserDto;
import com.codeit.sb01_deokhugam.global.dto.response.PageResponse;

public interface PowerUserService {

	PageResponse<PowerUserDto> findPowerUsers(GetPowerUsersRequest getPowerUsersRequest);

	void calculateAllPeriodRankings();
}
