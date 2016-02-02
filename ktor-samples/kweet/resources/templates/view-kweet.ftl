<#-- @ftlvariable name="kweet" type="kweet.model.Kweet" -->
<#import "template.ftl" as layout />

<@layout.mainLayout title="New kweet">
<h3>Kweet <small>(${kweet.id})</small></h3>
<p>Date: ${kweet.date.toDate()?string("yyyy.MM.dd HH:mm:ss")}</p>
<p>Text: </p>
<pre>
    ${kweet.text}
</pre>
</@layout.mainLayout>