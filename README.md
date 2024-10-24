# osti-elink

Wrapper library for managing DOIs through OSTI Elink

For more information, see: https://www.osti.gov/elink/

Notes for deploying the library supporting the v1 API:
1. Maintain the same configuration as the previous release
2. Replace the metacat/WEB-INF/lib/osti-elink-1.0.0.jar file with osti-elink-2.0.0.jar
3. Replace the metacat/style/common/osti/eml2osti.xsl file with the new version

Notes for deploying the library supporting the v2xml API
In addition to the file replacements mentioned above, configure three additional settings:
1. Base URL Configuration 
    - You can configure the base URL by setting the property `guid.doi.baseurl` in the
      `metacat.properties` file or by setting an environment variable named `guid.doi.baseurl`. The
      environment variable will override the property file setting.
    - Its value should be in the format: https://www.osti.gov
2. Service Class Name Configuration:
    - Set the environment variable `ostiService.className` to the value
      `edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService`
3. Token Configuration:
    - Set the environment variable `osti.token` with the appropriate token value

Notes for running maven tests
1. Clone the code from https://github.com/NCEAS/osti-elink
2. Install JDK 1.8 and maven
3. Edit the `src/test/resources/test.properties` file to point your credential files:
    ```
    ostiService.passwordFilePath=./password.properties
    ostiService.tokenPath=./token
    ```

    The password.properties file should be formatted as follows:
    ```
    guid.doi.username=your-username
    guid.doi.password=your-password
    ```
    The token file should contain only the OSTI token.
