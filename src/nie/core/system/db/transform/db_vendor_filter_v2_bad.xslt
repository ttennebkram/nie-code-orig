<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- The name of the database target we are using -->
<xsl:param name="db" />

<!--
<xsl:template match="db_variant">
	<xsl:if test="(not(@db_vendor) and not(@except_db_vendor)) or ((@db_vendor) and contains(@db_vendor,$db)) or ( (@except_db_vendor) and not( contains(@except_db_vendor,$db)) )">
		<xsl:copy>
			<xsl:apply-templates select="child::node()"/>
		</xsl:copy>
	</xsl:if>
</xsl:template>
-->

<xsl:template match="node() | @*">
	<xsl:if test="(not(@db_vendor) and not(@except_db_vendor)) or ((@db_vendor) and contains(@db_vendor,$db)) or ( (@except_db_vendor) and not( contains(@except_db_vendor,$db)) )">
		<xsl:for-each select="node()">
			<xsl:choose>
				<xsl:when test="not(self::db_variant)">
					<foo>
						<xsl:value-of select="." />
						<xsl:copy>
							<xsl:apply-templates select="child::node() | @*" />
						</xsl:copy>
					</foo>
					<!--
					<xsl:copy>
						<xsl:apply-templates select="self::node()" />
					</xsl:copy>
					-->
				</xsl:when>
				<xsl:otherwise>
					<!--
					<xsl:copy>
						<xsl:apply-templates select="node()" />
					</xsl:copy>
					-->
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>

		<!--
		<xsl:for-each select="@*">
		-->

	</xsl:if>
</xsl:template>


</xsl:stylesheet>
