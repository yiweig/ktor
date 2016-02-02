<#macro mainLayout title="Welcome">
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

    <title>${title} | Kweet</title>
    <link rel="stylesheet" type="text/css" href="/css/style.css">
</head>
<body>
<div class="page">
    <h1>Kweet</h1>
    <div class="navigation">
        <#if user??>
            <a href="/">my timeline</a> |
            <a href="/public">public timeline</a> |
            <a href="/logout">sign out [${user.username}]</a>
        <#else>
            <a href="/public">public timeline</a> |
            <a href="/register">sign up</a> |
            <a href="/login">sign in</a>
        </#if>
    </div>
    <div class="body">
        <h2>${title}</h2>
        <#nested />
    </div>
    <div class="footer">
        Kweet ktor example, ${.now?string["yyyy"]}
    </div>
</div>
</body>
</html>
</#macro>