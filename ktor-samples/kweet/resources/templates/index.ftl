<#-- @ftlvariable name="top" type="java.util.List<kweet.model.Kweet>" -->
<#import "template.ftl" as layout />

<@layout.mainLayout title="Welcome">
<h3>Top 10</h3>

<ul>
    <#list top as kweet>
        <li><a href="/kweet/${kweet.id}">${kweet.text}</a> (by ${kweet.userId})</li>
    <#else>
        <li>There are no kweets yet</li>
    </#list>
</ul>
</@layout.mainLayout>
