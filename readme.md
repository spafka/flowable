### 服务任务 向外 推送

需要 外部的接口地址，认证信息，同一向外post请求。
其中formData为表单数据,processId 为当前流程的id，triggerKey为下个接收任务的节点ID
```json
{
  "formData": "{\"time\":\"2\",\"ctrlIndex\":\"htDiv-li055cju0-194\",\"ctrlType\":\"0\"}", 
  "processId": 12342342341,
  "triggerKey":"ReciverTask_123234123"
}
```
                                                                                       
### openapi 工作流接收任务

暂定接口地址 /open-api/p/workflow/v2/tasks/recivertask 

其中formData为表单数据,processId 为当前流程的id，triggerKey为当前接收任务的节点ID
```json
{
  "formData": "{\"time\":\"2\",\"ctrlIndex\":\"htDiv-li055cju0-194\",\"ctrlType\":\"0\"}",
  "processId": 12342342341,
  "triggerKey":"ReciverTask_123234123"
}
```
工作流接收请求，接收任务，工作流继续流转



1. ide 新建启用工作流时，已appId，appName，工作流服务注册接口，向工作流注册工作流服务。
2. ide 新增或删除工作流页面时，调工作流页面新增或删除openapi
3. ide 新增或删除工作流页面时，需调工作页面新增接口，其中接口中的表单字段为当前页面的全量表单字段。

