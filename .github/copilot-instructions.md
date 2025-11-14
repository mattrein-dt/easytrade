When generating DQL for Dynatrace, always use the natural language to dql tool first.

IMPORTANT: Before executing ANY DQL query using the execute_dql tool, you MUST first run the generate_dql_from_natural_language tool to convert the request into proper DQL syntax. Never execute DQL directly without this step.

# When reviewing dynatrace problems

FOCUS ON Code issues and ignore infrastructure issues.


1. Use the `list_problems` tool to get a list of recent problems (prefer this tool over DQL). Note that problems with event.status ACTIVE are currently active problems, while problems with event.status CLOSED are NOT active.
2. Review the problems for relevance - a problem is relevant if it relates to classes, namespaces, files, or code within the current #codebase.
3. Get details for relevant problems using DQL, including:
   - Root cause information
   - Affected entities
   - Problem duration/timeline
   - Related events
4. Search for ERROR and WARN level logs by filtering the content field for controller names (e.g., "OrderController", "AccountController") and other relevant information from the codebase. Skip searching for logs by specific service entities.
5. Concisely summarize the problem and resolution, then provide specific code changes to fix the issue.
6. Optionally recommend additional steps like reviewing traces if helpful for diagnosis.