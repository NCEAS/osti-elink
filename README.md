# OSTI-elink

Wrapper library for managing DOIs through OSTI Elink

For more information, see: https://www.osti.gov/elink/

### Notes for deploying the library supporting the v1 API:
1. Maintain the same Metacat configuration as the previous release
2. Replace the metacat/WEB-INF/lib/osti-elink-1.0.0.jar file with osti-elink-2.0.0.jar
3. Replace the metacat/style/common/osti/eml2osti.xsl file with the new version

### Notes for deploying the library supporting the v2xml API
In addition to the file replacements mentioned above, configure four additional settings:
1. Service Class Name Configuration:
   - Set the environment variable `METACAT_OSTI_SERVICE_CLASS_NAME` to the value
     `edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService`
2. Base URL Configuration:
    - You can configure the base URL by setting the property `guid.doi.baseurl` in the
      `metacat.properties` file or by setting an environment variable named `METACAT_OSTI_BASE_URL`. The
      environment variable will override the property file setting.
    - Its value should be in the format: `https://www.osti.gov` (production) or `https://review.osti.gov` (test)
3. Token Configuration:
    - Set the environment variable `METACAT_OSTI_TOKEN` with the appropriate token value
4. Context name configuration (Optional):
   - Default Context Names: OSTI ensures that the context names `elink2xml` (for the v2 XML API)
   and `elink2api` (for the v2 JSON API) remain the same across both test and production services.
   These values are set as defaults in the configuration.
   - Customizing Context Names: If you need to change these context names, you can configure them
     through the following environment variables:
     - `METACAT_OSTI_V2XML_CONTEXT` for the v2 XML API
     - `METACAT_OSTI_V2JSON_CONTEXT` for the v2 JSON API
5. Max query-attempts configuration (Optional):
    - There is a delay between minting a DOI and its search availability in the OSTI service. This
      delay typically ranges from one to five seconds. To account for this, the system
      repeatedly queries the service, waiting 200 milliseconds after each failed attempt. By
      default, the maximum number of query attempts is set to 40, resulting in a maximum
      waiting time of eight seconds.
    - You can customize the maximum number of query attempts by setting the environment variable
      `METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS`.

### Notes for running maven test
1. Clone the code from https://github.com/NCEAS/osti-elink
2. Install JDK 1.8 and maven
3. Edit the `src/test/resources/test.properties` file to point your credential files:
    ```
    ostiService.v1.passwordFilePath=./password.properties
    ostiService.v2.tokenFilePath=./token
    ```

    The password.properties file should be formatted as follows:
    ```
    guid.doi.username=your-username
    guid.doi.password=your-password
    ```
    The token file should contain only the OSTI token.
