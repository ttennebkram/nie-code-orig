<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- The name of the database target we are using -->
<xsl:param name="db" />

<xsl:template match="node() | @*">
	<xsl:if test="(not(@db_vendor) and not(@except_db_vendor)) or ((@db_vendor) and contains(@db_vendor,$db)) or ( (@except_db_vendor) and not( contains(@except_db_vendor,$db)) )">
		<xsl:copy>
			<xsl:apply-templates select="node() | @*"/>
		</xsl:copy>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>
