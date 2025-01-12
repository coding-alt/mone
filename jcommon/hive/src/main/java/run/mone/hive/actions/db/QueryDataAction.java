
package run.mone.hive.actions.db;

import com.google.common.collect.ImmutableMap;
import run.mone.hive.actions.Action;
import run.mone.hive.common.AiTemplate;
import run.mone.hive.schema.ActionContext;
import run.mone.hive.schema.ActionReq;
import run.mone.hive.schema.Message;

public class QueryDataAction extends Action {

    public QueryDataAction() {
        super("QueryData", "Execute database queries and return results");
        this.function = this::queryData;
    }

    private Message queryData(ActionReq req, Action action, ActionContext context) {
        String queryResult = "Here are the results of your query:\n" +
                "${requirements}";

        String prompt = AiTemplate.renderTemplate(queryResult, ImmutableMap.of("requirements", req.getMessage().getContent()));
        String res = llm.chat(prompt);

        return Message.builder()
                .role(req.getRole().getName())
                .content(res)
                .build();
    }
}
