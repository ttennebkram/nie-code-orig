<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- <xsl:output method="text"/> -->

<!-- The name of the database target we are using -->
<xsl:param name="db" />
<xsl:param name="ignore_error" />

<!-- The root -->
<xsl:template match="/">

<statements>

<!-- Preamble: dropping old versions of the tables, etc -->
<xsl:apply-templates select="table" mode="preamble" />

<!-- Create fields: the actual create statement, types -->
<!-- Indicies and constraints -->


<!-- closing -->

</statements>

</xsl:template>


<!-- Preamble:
	==========================================
-->
<xsl:template match="table" mode="preamble">
<statement>
	<xsl:attribute name="ignore_error"><xsl:value-of select="$ignore_error" /></xsl:attribute>
	DROP TABLE <xsl:value-of select="@name" />
	<xsl:if test="$db = 'oracle'">
		CASCADE CONSTRAINTS
	</xsl:if>
</statement>
</xsl:template>

</xsl:stylesheet>
