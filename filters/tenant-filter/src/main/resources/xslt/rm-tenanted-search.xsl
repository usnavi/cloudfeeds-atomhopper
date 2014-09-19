<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">

    <xsl:param name="tenantId"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="stripTenantedSearch">
        <xsl:param name="url"/>

        <xsl:choose>
            <!-- if there is a tenant id in the URL, we need to remove the tenanted-feed generated search terms -->
            <xsl:when test="$tenantId != ''">
                <xsl:variable name="tenant_url">
                    <xsl:value-of select="replace( $url, '/events', concat( '/events/', $tenantId ) )"/>
                </xsl:variable>

                <xsl:variable name="defaultSearch">
                    <xsl:value-of select="concat( concat( '&amp;search=%28AND%28cat%3Dtid%3A', $tenantId ), '%29%28NOT%28cat%3Dcloudfeeds%3Aprivate%29%29%29' )" />
                </xsl:variable>

                <xsl:variable name="customSearch">
                    <xsl:value-of select="concat( concat( '&amp;search=%28AND%28AND%28cat%3Dtid%3A', $tenantId ), '%29%28NOT%28cat%3Dcloudfeeds%3Aprivate%29%29%29' )" />
                </xsl:variable>
                <!-- remove added tenanted feed search -->
                <xsl:choose>
                    <xsl:when test="contains( $tenant_url, $defaultSearch )">
                        <xsl:value-of select="replace( $tenant_url, $defaultSearch, '' )"/>
                    </xsl:when>
                    <xsl:when test="contains( $tenant_url, $customSearch )">

                        <xsl:variable name="searchRegex">
                            <xsl:value-of select="concat( concat( '.*search=%28AND%28AND%28cat%3Dtid%3A', $tenantId ), '%29%28NOT%28cat%3Dcloudfeeds%3Aprivate%29%29%29(.*)%29.*' )" />
                        </xsl:variable>

                        <xsl:variable name="newSearch">
                            <xsl:analyze-string select="$url" regex="{$searchRegex}">
                                <xsl:matching-substring>
                                    <xsl:value-of select="regex-group(1)"/>
                                </xsl:matching-substring>
                            </xsl:analyze-string>
                        </xsl:variable>

                        <xsl:value-of select="replace( $tenant_url, 'search=[^&amp;]+', concat( 'search=', $newSearch ))"/>
                    </xsl:when>
                   <xsl:otherwise>
                        <xsl:value-of select="$tenant_url"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$url"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="@href[parent::node()[@rel='previous' or @rel='next' or @rel='current' or @rel='self' or @rel='last']]">
        <xsl:attribute name="href">
            <xsl:call-template name="stripTenantedSearch">
                <xsl:with-param name="url" select="."></xsl:with-param>
            </xsl:call-template>
        </xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
