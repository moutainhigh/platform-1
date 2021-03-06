package com.kunlong.metadata.service.server;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kunlong.metadata.model.*;
import com.kunlong.metadata.service.DictDataTypeService;
import com.kunlong.metadata.service.MetadataDictService;
import com.kunlong.metadata.service.MetadataFieldService;
import com.kunlong.metadata.service.SysMetaDataService;
import com.kunlong.metadata.service.impl.*;
import com.kunlong.mybatis.KunlongSql;
import com.kunlong.platform.context.RestMessage.MsgRequest;
import com.kunlong.platform.context.RestMessage.MsgResponse;
import com.kunlong.platform.context.rest.RestHandler;
import com.kunlong.platform.domain.SubsysDict;
import com.kunlong.platform.model.KunlongError;
import com.kunlong.platform.utils.KunlongUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;

@Component
public class MetadataDictServer {

    private static final Logger logger = LoggerFactory.getLogger(MetadataDictServer.class);
    @Autowired
    DictDataTypeService dictDataTypeService;//= new DictDataTypeServiceImpl();

    @Autowired
    MetaDataService metaDataService;
    @Autowired
    SysMetaDataService sysMetaDataService;
    @Autowired
    MetadataDictService metadataDictService;

    @Autowired
    MetadataFieldService metadataFieldService;//aFieldServiceImpl();

    int retcode = 0;
    String retmsg = "成功";
    String msgBody = "{}";

    public MsgResponse updateFieldOrder(MsgRequest req, RestHandler handler) {
        String fieldIds = req.msgBody.getString("fieldIds");
        int field_order = 1;
        for (String fieldId : fieldIds.split(",")) {
            StringBuilder sql = new StringBuilder();
            sql.append("update ytb_manager.metadata_field set field_order=").append(field_order++);
            sql.append(" where field_id=").append(fieldId);
            KunlongSql.update(sql);
        }
        msgBody = "{}";
        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getCachedTableList(MsgRequest req, RestHandler handler) {

        StringBuilder sql = new StringBuilder();
        sql.append("select metadata_alias,metadata_name from ytb_manager.metadata_dict ");
        sql.append(" where metadata_cached=1");
        List<MetadataDict> lst = KunlongSql.selectList(sql, MetadataDict.class);
        msgBody = "{'list':" + KunlongUtils.toJSONStringSkipNull(lst) + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);

    }

    public MsgResponse checkDict(MsgRequest req, RestHandler handler) {

        Integer metadataId = req.msgBody.getInteger("metadataId");
        StringBuilder sql = new StringBuilder();
        sql.append("call ytb_manager.spCheckMetadata");
        sql.append("(").append(metadataId).append(")");
        List<Map<String, Object>> lst = KunlongSql.selectList(sql);
        msgBody = "{'list':" + KunlongUtils.toJSONStringPretty(lst) + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);
    }

    public MsgResponse getDictList(MsgRequest req, RestHandler handler) {

        MetadataDictExample metadataDictExample = new MetadataDictExample();
        MetadataDictExample.Criteria criteria = metadataDictExample.createCriteria();

        if (req.msgBody.getInteger("metadataId") != null) {
            criteria.andMetadataIdEqualTo(req.msgBody.getInteger("metadataId"));
        }
        if (req.msgBody.getInteger("subsysId") != null) {
            criteria.andSubsysIdEqualTo(req.msgBody.getInteger("subsysId"));
        }
        if (req.msgBody.getInteger("metadataType") != null) {
            criteria.andMetadataTypeEqualTo(req.msgBody.getInteger("metadataType"));
        }
        metadataDictExample.setOrderByClause("metadata_order asc");
        List<MetadataDict> sysMetaDataDictModelList = metadataDictService.selectByExample(metadataDictExample);
        JSONArray array = JSONArray.parseArray(JSON.toJSONString(sysMetaDataDictModelList));

        msgBody = "{\"list\":" + array + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getDictFieldsDefaultValueList(MsgRequest req, RestHandler handler) {

        int metadataId = 109;//req.msgBody.getInteger("metadataId");
        MetadataFieldServiceImpl mdfService = new MetadataFieldServiceImpl();
        MetadataFieldExample mdfExample = new MetadataFieldExample();
        mdfExample.createCriteria().andMetadataIdEqualTo(metadataId);
        mdfExample.setOrderByClause("field_order asc");
        List<MetadataField> lst = metadataFieldService.selectByExample(mdfExample);
        msgBody = "{\"list\":" + JSONObject.toJSONString(lst) + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);
    }

    public MsgResponse getDictListAndField(MsgRequest req, RestHandler handler) {

        int metadataId = req.msgBody.getInteger("metadataId");

        List<MetadataDict> metadataDicts = null;
        JSONArray json = new JSONArray();

        MetadataDictExample metadataDictExample = new MetadataDictExample();
        metadataDictExample.createCriteria().andMetadataIdEqualTo(metadataId);
        metadataDictExample.setOrderByClause("metadata_order asc");
        metadataDicts = metadataDictService.selectByExample(metadataDictExample);

        if (metadataDicts.size() > 0) {
            for (MetadataDict metadataDict : metadataDicts) {

                MetadataFieldExample metadataFieldExample = new MetadataFieldExample();
                metadataFieldExample.createCriteria().andMetadataIdEqualTo(metadataDicts.get(0).getMetadataId());
                metadataFieldExample.setOrderByClause("field_order asc");
                List<MetadataField> sysMetaDataFieldModelList = metadataFieldService.selectByExample(metadataFieldExample);
                metadataDict.setField(sysMetaDataFieldModelList);
            }


            json.add(metadataDicts);
            /*      String jsonString = JSONObject.toJSONString(metadataDicts, SerializerFeature.WriteMapNullValue);*/

            msgBody = "{\"list\":" + JSONObject.toJSONString(json, SerializerFeature.WriteMapNullValue) + "}";

        }
        msgBody = "{\"list\":" + JSONObject.toJSONString(json, SerializerFeature.WriteMapNullValue) + "}";


        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getDictTableAndField(MsgRequest req, RestHandler handler) {
        String metadataName = req.msgBody.getString("metadataName");

        List<MetadataDict> lst = metaDataService.getDictTableAndField(metadataName);
        msgBody = "{\"list\":" + JSONObject.toJSONString(lst, SerializerFeature.WriteMapNullValue) + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getDictListByType(MsgRequest req, RestHandler handler) {

        Integer metadataType = req.msgBody.getInteger("metadataType");
        MetadataDictExample metadataDictExample = new MetadataDictExample();
        metadataDictExample.createCriteria().andMetadataTypeEqualTo(metadataType);
        List<MetadataDict> metadataDicts = metadataDictService.selectByExample(metadataDictExample);


        msgBody = "{\"list\":" + JSONObject.toJSONString(metadataDicts, SerializerFeature.WriteMapNullValue) + "}";


        return handler.buildMsg(retcode, retmsg, msgBody);
    }

    public MsgResponse getDictListAndFieldByType(MsgRequest req, RestHandler handler) {

        Integer metadataType = req.msgBody.getInteger("metadataType");

        MetadataDictExample metadataDictExample = new MetadataDictExample();
        metadataDictExample.createCriteria().andMetadataTypeEqualTo(metadataType);
        List<MetadataDict> metadataDicts = metadataDictService.selectByExample(metadataDictExample);


        if (metadataDicts.size() > 0) {
            for (MetadataDict sysMetaDataDictModel : metadataDicts) {

                MetadataFieldExample metadataFieldExample = new MetadataFieldExample();
                metadataFieldExample.createCriteria().andMetadataIdEqualTo(sysMetaDataDictModel.getMetadataId());
                List<MetadataField> metadataFields = metadataFieldService.selectByExample(metadataFieldExample);
                sysMetaDataDictModel.setField(metadataFields);
            }

        }
        msgBody = "{\"list\":" + JSONObject.toJSONString(metadataDicts, SerializerFeature.WriteMapNullValue) + "}";

        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getDictAndFieldByType(MsgRequest req, RestHandler handler) {

        Integer metadataId = req.msgBody.getInteger("metadataId");
        MetadataDict sysMetaDataDictModel = metadataDictService.selectByPrimaryKey(metadataId);


        if (sysMetaDataDictModel != null) {
            MetadataFieldExample metadataFieldExample = new MetadataFieldExample();
            metadataFieldExample.createCriteria().andMetadataIdEqualTo(metadataId);
            List<MetadataField> metadataFields = metadataFieldService.selectByExample(metadataFieldExample);
            sysMetaDataDictModel.setField(metadataFields);
        }

        JSONObject json = new JSONObject();
        json.put("metaDataDicModel", sysMetaDataDictModel);

        msgBody = "{\"list\":" + JSONObject.toJSONString(json, SerializerFeature.WriteMapNullValue) + "}";


        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getDictMetadataName(MsgRequest req, RestHandler handler) {

        try {

            if (req.msgBody.getString("metadataName") != null && req.msgBody.getString("metadataName") != "") {

                MetadataDictExample example = new MetadataDictExample();
                example.createCriteria().andMetadataNameEqualTo(req.msgBody.getString("metadataName"));
                List<MetadataDict> sysMetaDataFieldModelList = sysMetaDataService.selectByExample(example);
                JSONArray array = JSONArray.parseArray(JSON.toJSONString(sysMetaDataFieldModelList));
                msgBody = "{\"list\":" + JSONObject.toJSONString(array, SerializerFeature.WriteMapNullValue) + "}";

            } else {
                JSONObject json = new JSONObject();
                retcode = 1;
                retmsg = "metadataName 不得为空";
                msgBody = "{\"list\":" + JSONObject.toJSONString(json, SerializerFeature.WriteMapNullValue) + "}";
            }
        } catch (Exception e) {
            retcode = 1;
            retmsg = "数据错误";
        }


        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getDictAndFieldByMetadataName(MsgRequest req, RestHandler handler) {

        try {

            if (req.msgBody.getString("metadataName") != null && req.msgBody.getString("orderBy") != null) {

                if (req.msgBody.getString("orderBy").equals("desc") || req.msgBody.getString("orderBy").equals("asc")) {

                    MetadataDictExample example = new MetadataDictExample();
                    example.setOrderByClause("metadata_order  " + req.msgBody.getString("orderBy") + " ");
                    example.createCriteria().andMetadataNameEqualTo(req.msgBody.getString("metadataName"));
                    List<MetadataDict> sysMetaDataFieldModelList = sysMetaDataService.selectByExample(example);


                    if (sysMetaDataFieldModelList.size() > 0) {
                        for (int i = 0; i < sysMetaDataFieldModelList.size(); i++) {
                            MetadataFieldExample mbe = new MetadataFieldExample();
                            MetadataFieldExample.Criteria criteria = mbe.createCriteria();
                            criteria.andMetadataIdEqualTo(sysMetaDataFieldModelList.get(i).getMetadataId());
                            mbe.setOrderByClause("field_order " + req.msgBody.getString("orderBy") + " ");
                            List<MetadataField> FetadataField = metadataFieldService.selectByExample(mbe);


                            sysMetaDataFieldModelList.get(i).setField(FetadataField);
                        }
                    }


                    JSONArray array = JSONArray.parseArray(JSON.toJSONString(sysMetaDataFieldModelList));
                    msgBody = "{\"list\":" + array.toJSONString() + "}";
                } else {
                    retcode = 1;
                    retmsg = "orderBy 的排序方式只能为 desc 或 asc";

                }
            } else {
                JSONObject json = new JSONObject();
                retcode = 1;
                retmsg = "metadataName 不得为空";
                msgBody = "{\"list\":" + json.toJSONString() + "}";
            }
        } catch (Exception e) {
            retcode = 1;
            retmsg = "数据错误";
        }


        return handler.buildMsg(retcode, retmsg, msgBody);
    }

    public MsgResponse dictByInsertSelective(MsgRequest req, RestHandler handler) {

        MetadataDict metadataDict = JSONObject.parseObject(req.msgBody.toString(), MetadataDict.class);
        System.err.println(JSONObject.toJSONString(req));
        int id = metadataDictService.insertSelective(metadataDict);
        String msgBody = "{'id':" + id + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);

    }

    public MsgResponse copyMaster(int mid, RestHandler handler) {
        int id = metadataDictService.copyMaster(mid);
        String msgBody = "{'id':" + id + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);

    }

    public MsgResponse dpMaster(int mid, RestHandler handler) {
        int id = metadataDictService.dpMaster(mid);
        String msgBody = "{'id':" + id + "}";
        if (id == 0) {
            throw new KunlongError(KunlongError.CODE_DEFINE_ERROR, " 有表记录不能删除!");
        }
        return handler.buildMsg(retcode, retmsg, msgBody);

    }

    public MsgResponse dictByUpdateByKey(MsgRequest req, RestHandler handler) {

        MetadataDict metadataDict = JSONObject.parseObject(req.msgBody.toString(), MetadataDict.class);
        int id = metadataDictService.updateByPrimaryKeySelective(metadataDict);
        String msgBody = "{'id':" + id + "}";
        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse dictDeleteByKey(MsgRequest req, RestHandler handler) {

        try {
            Integer metadataId = Integer.parseInt(req.msgBody.getString("metadataId"));
            if (metadataId != null) {

                /*     MetadataDict metadataDict = metadataDictService.selectByPrimaryKey(metadataId);*/

                MetadataFieldExample metadataFieldExample = new MetadataFieldExample();
                metadataFieldExample.createCriteria().andMetadataIdEqualTo(metadataId);


                List<MetadataField> metadataFields = metadataFieldService.selectByExample(metadataFieldExample);

                if (metadataFields.size() > 0) {
                    retcode = 1;
                    retmsg = "操作失败 子表有数据时不能删除表";
                    msgBody = "{}";
                } else {


                    int sta = metadataDictService.deleteByPrimaryKey(metadataId);
                    if (sta > 0) {

                    } else {
                        retcode = 1;
                        retmsg = "操作删除数据失败";
                        msgBody = "{}";

                    }
                }

            }
        } catch (Exception e) {

        }
        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse dictByUpdate(MsgRequest req, RestHandler handler) {
        try {
            MetadataDict metadataDict = JSONObject.parseObject(req.msgBody.toString(), MetadataDict.class);


            MetadataDictExample metadataDictExample = new MetadataDictExample();

            MetadataDictExample.Criteria criteria = metadataDictExample.createCriteria();
            if (metadataDict.getMetadataId() != null) {
                criteria.andMetadataIdEqualTo(metadataDict.getMetadataId());
            }

            if (metadataDict.getSubsysId() != null) {
                criteria.andMetadataIdEqualTo(metadataDict.getSubsysId());
            }
            int sysDictDataTypeModel = metadataDictService.updateByExampleSelective(metadataDict, metadataDictExample);

            if (sysDictDataTypeModel >= 0) {

            } else {
                retcode = 1;
                retmsg = "操作失败";
                msgBody = "{}";
            }


        } catch (Exception e) {
            retcode = 1;
            retmsg = "操作失败";
            msgBody = "{}";

        }
        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse dictBySelectByKey(MsgRequest req, RestHandler handler) {
        try {
            Integer metadataId = Integer.parseInt(req.msgBody.getString("metadataId"));
            if (metadataId != null) {
                MetadataDict sta = metadataDictService.selectByPrimaryKey(metadataId);

                JSONObject array = JSONObject.parseObject(JSON.toJSONString(sta));


                msgBody = "{\"list\":" + array + "}";
            } else {
                retcode = 1;
                retmsg = "操作失败";
                msgBody = "{}";
            }
        } catch (Exception e) {
            retcode = 1;
            retmsg = "操作失败";
            msgBody = e.toString();

        }

        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse getDictById(MsgRequest req, RestHandler handler) {
        Integer metadataType = req.msgBody.getInteger("metadataId");
        MetadataDict metadataDicts = metadataDictService.selectByPrimaryKey(metadataType);


        JSONObject json = new JSONObject();
        json.put("metaDataDicModel", JSONObject.toJSONString(metadataDicts, SerializerFeature.WriteMapNullValue));

        msgBody = "{\"list\":" + json.toJSONString() + "}";

        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse deleteDictById(MsgRequest req, RestHandler handler) {
        int metadataId = req.msgBody.getInteger("metadataId");
        //删除主表的数据
        sysMetaDataService.deleteDictById(metadataId);
        //删除该表对应的所有的字段
        sysMetaDataService.deleteFieldsByDictId(metadataId);
        return handler.buildMsg(retcode, retmsg, msgBody);
    }

/*
    public MsgResponse getSubSysDictListOld(MsgRequest req, RestHandler handler) {
        SysMetaDataServiceImpl sysMetaDataService = new SysMetaDataServiceImpl();

        List<Sub_SysDictModel> subSysDictList = sysMetaDataService.getSubSysDictList();
        JSONArray json = new JSONArray();
        for (Sub_SysDictModel sysDictDataTypeModel : subSysDictList) {
            json.add(sysDictDataTypeModel);
        }
        msgBody = "{\"list\":" + json.toJSONString() + "}";

        return handler.buildMsg(retcode, retmsg, msgBody);
    }*/


    public MsgResponse getSubSysDictList(MsgRequest req, RestHandler handler) {

        List<SubsysDict> subSysDictList = sysMetaDataService.getSubSysDictList();

        msgBody = "{\"list\":" + JSONObject.toJSONString(subSysDictList) + "}";
        logger.info("msgBody: {}",KunlongUtils.toJSONStringPretty(subSysDictList));
        return handler.buildMsg(retcode, retmsg, msgBody);
    }


    public MsgResponse addDictDataType(MsgRequest req, RestHandler handler) {
        DictDatatype dictDatatype = req.msgBody.toJavaObject(req.msgBody, DictDatatype.class);
        dictDataTypeService.insert(dictDatatype);
        return handler.buildMsg(retcode, retmsg, msgBody);

    }


    public MsgResponse updateDictDataTypeById(MsgRequest req, RestHandler handler) {

        DictDatatype dictDatatype = req.msgBody.toJavaObject(req.msgBody, DictDatatype.class);
        dictDataTypeService.updateByPrimaryKey(dictDatatype);
        return handler.buildMsg(retcode, retmsg, msgBody);

    }

    public MsgResponse deleteDictDataTypeById(MsgRequest req, RestHandler handler) {
        int dataInnerId = Integer.parseInt(req.msgBody.getString("dataInnerId"));
        DictDatatype dictDatatype = req.msgBody.toJavaObject(req.msgBody, DictDatatype.class);
        dictDataTypeService.deleteByPrimaryKey(dataInnerId);

        return handler.buildMsg(retcode, retmsg, msgBody);

    }


}





