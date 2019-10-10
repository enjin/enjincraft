package com.enjin.enjincraft.spigot;

import com.enjin.sdk.graphql.GraphQLError;

import java.util.List;

public class GraphQLException extends RuntimeException {

    private static final String GQL_NO_ERRORS_MESSAGE = "GraphQL Response Failed w/ No Errors";
    private static final String GQL_ERRORS_HEADER = "GraphQL Errors:\n";

    public GraphQLException(List<GraphQLError> errors) {
        super(generateMessage(errors));
    }

    private static String generateMessage(List<GraphQLError> errors) {
        StringBuilder builder = new StringBuilder();

        if (errors == null || errors.size() == 0) {
            builder.append(GQL_NO_ERRORS_MESSAGE);
        } else {
            builder.append(GQL_ERRORS_HEADER);
            int num = 1;
            for (GraphQLError error : errors) {
                if (error == null) continue;
                builder.append(String.format("%s - %s", num++, error.toString()));
            }
        }

        return builder.toString();
    }

}
