<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fits="http://hul.harvard.edu/ois/xml/ns/fits/fits_output" xmlns:uuid="java:java.util.UUID" version="2.0">
  <xsl:template match="/">
    <premis:object xmlns:premis="info:lc/xmlns/premis-v2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="premis:file">
      <premis:objetIdentifier>
        <premis:objectIdentifierType>uuid</premis:objectIdentifierType>
        <premis:objectIdentifierValue><xsl:value-of select="uuid:randomUUID()"/></premis:objectIdentifierValue>
      </premis:objetIdentifier>
      <premis:objectCharacteristics>
        <premis:compositionLevel>0</premis:compositionLevel>
        <premis:fixity>
          <premis:messageDigestAlgorithm>MD5</premis:messageDigestAlgorithm>
          <premis:messageDigest>
            <xsl:value-of select="/fits:fits/fits:fileinfo/fits:md5checksum"/>
          </premis:messageDigest>
          <premis:messageDigestOriginator>
            <xsl:value-of select="/fits:fits/fits:fileinfo/fits:md5checksum/@toolname"/>
          </premis:messageDigestOriginator>
        </premis:fixity>
        <premis:size>
          <xsl:value-of select="/fits:fits/fits:fileinfo/fits:size"/>
        </premis:size>
        <xsl:for-each select="/fits:fits/fits:identification/fits:identity">
          <!--TODO-->
          <premis:format>
            <premis:formatDesignation>
              <premis:formatName>
                <xsl:value-of select="@format"/>
              </premis:formatName>
              <xsl:if test="fits:version">
                <premis:formatVersion>
                  <xsl:value-of select="fits:version"/>
                </premis:formatVersion>
              </xsl:if>
            </premis:formatDesignation>
            <!--TODO: Add format registry -->
            <xsl:if test="fits:externalIdentifier/@toolname='Droid'">
              <premis:formatRegistry>
                <premis:formatRegistryName>http://www.nationalarchives.gov.uk/pronom</premis:formatRegistryName>
                <premis:formatRegistryKey><xsl:value-of select="fits:externalIdentifier" /></premis:formatRegistryKey>
              </premis:formatRegistry>
            </xsl:if>
            <xsl:for-each select="fits:tool">
              <premis:formatNote>Identified by: <xsl:value-of select="@toolname"/><xsl:text> v</xsl:text><xsl:value-of select="@toolversion"/></premis:formatNote>
            </xsl:for-each>
          </premis:format>
        </xsl:for-each>
      </premis:objectCharacteristics>
      <premis:originalName>
        <xsl:value-of select="/fits:fits/fits:fileinfo/fits:filename"/>
      </premis:originalName>
    </premis:object>
  </xsl:template>
</xsl:stylesheet>
