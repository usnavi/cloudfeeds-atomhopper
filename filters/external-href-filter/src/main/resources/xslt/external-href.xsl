<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">

    <xsl:param name="correct_url"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="replaceHostnameInUrl">
        <xsl:param name="url"/>

        <xsl:variable name="org_hostname">
            <xsl:analyze-string select="$url" regex="(https?://[^/:]+)/.*">
                <xsl:matching-substring>
                    <xsl:value-of select="regex-group(1)"/>
                </xsl:matching-substring>
            </xsl:analyze-string>
        </xsl:variable>
        <xsl:value-of select="replace( $url, $org_hostname, $correct_url )"/>
    </xsl:template>

    <xsl:template match="@href">
        <xsl:attribute name="href">
            <xsl:call-template name="replaceHostnameInUrl">
                <xsl:with-param name="url" select="."></xsl:with-param>
            </xsl:call-template>
        </xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
