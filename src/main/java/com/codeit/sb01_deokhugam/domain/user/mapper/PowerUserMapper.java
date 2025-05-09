package com.codeit.sb01_deokhugam.domain.user.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.codeit.sb01_deokhugam.domain.user.dto.response.PowerUserDto;
import com.codeit.sb01_deokhugam.domain.user.entity.PowerUser;

@Mapper(componentModel = "spring")
public interface PowerUserMapper {
	PowerUserDto toDto(PowerUser powerUser);

	List<PowerUserDto> toDtoList(List<PowerUser> powerUsers);
}
