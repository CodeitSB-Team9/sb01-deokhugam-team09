package com.codeit.sb01_deokhugam.domain.user.repository;

import java.util.List;

import com.codeit.sb01_deokhugam.domain.user.dto.request.GetPowerUsersRequest;
import com.codeit.sb01_deokhugam.domain.user.entity.PowerUser;

public interface PowerUserSearchRepository {
	List<PowerUser> findPowerUsers(GetPowerUsersRequest getPowerUsersRequest);
}
