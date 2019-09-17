package com.osdu.mapper;

import com.osdu.model.delfi.DelfiSearchObject;
import com.osdu.model.delfi.geo.exception.GeoLocationException;
import com.osdu.model.osdu.OSDUSearchObject;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
@DecoratedWith(DelfiSearchObjectMapperDecorator.class)
public interface SearchObjectMapper {


    @Mapping(source = "osduSearchObject.count", target = "limit")
    @Mapping(source = "osduSearchObject.start", target = "offset")
    @Mapping(source = "osduSearchObject.facets", target = "returnedFields")
    @Mapping(target = "sort", ignore = true)
    DelfiSearchObject osduSearchObjectToDelfiSearchObject(OSDUSearchObject osduSearchObject, String kindOverride, String partitionOverride) throws GeoLocationException;


}