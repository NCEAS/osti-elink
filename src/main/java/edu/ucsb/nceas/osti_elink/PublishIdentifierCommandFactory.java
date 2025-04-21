//package edu.ucsb.nceas.osti_elink;
//
//import edu.ucsb.nceas.osti_elink.v1.OSTIService;
//import edu.ucsb.nceas.osti_elink.v2.json.OstiV2JsonService;
//import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService;
//
///**
// * Factory class to generate the PublishIdentifierCommand object
// * @author Tao
// */
//public class PublishIdentifierCommandFactory {
//
//    /**
//     * Generate the PublishIdentifierCommand object based on the given service object
//     * @param service  the osti service class
//     * @return the PublishIdentifierCommand associated with the osti service class
//     * @throws OSTIElinkException
//     */
//    public static PublishIdentifierCommand getInstance(OSTIElinkService service)
//        throws OSTIElinkException {
//        if (service instanceof OSTIService) {
//            return new edu.ucsb.nceas.osti_elink.v1.PublishIdentifierCommand();
//        } else if (service instanceof OSTIv2XmlService) {
//            return new edu.ucsb.nceas.osti_elink.v2.xml.PublishIdentifierCommand();
//        } else if (service instanceof OstiV2JsonService) {
//            return new edu.ucsb.nceas.osti_elink.v2.json.PublishIdentifierCommand();
//        } else {
//            throw new OSTIElinkException(
//                "OSTI library does not support the class " + service.getClass().getName()
//                    + " to generate the PublishIdentifierCommand class.");
//        }
//    }
//}
