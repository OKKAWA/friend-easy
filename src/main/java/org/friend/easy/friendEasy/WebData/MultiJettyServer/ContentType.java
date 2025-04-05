package org.friend.easy.friendEasy.WebData.MultiJettyServer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ContentType {
    private final String type;
    private final String subtype;
    private final Map<String, String> parameters;
    public interface SimpleContentType{
        final String CONTENT_JSON = "application/json";
        final String CONTENT_STAR = "application/octet-stream";
        final String CONTENT_TIF = "image/tiff";
        final String CONTENT_X001 = "application/x-001";
        final String CONTENT_X301 = "application/x-301";
        final String CONTENT_X323 = "text/h323";
        final String CONTENT_X906 = "application/x-906";
        final String CONTENT_X907 = "drawing/907";
        final String CONTENT_A11 = "application/x-a11";
        final String CONTENT_ACP = "audio/x-mei-aac";
        final String CONTENT_AI = "application/postscript";
        final String CONTENT_AIF = "audio/aiff";
        final String CONTENT_AIFC = "audio/aiff";
        final String CONTENT_AIFF = "audio/aiff";
        final String CONTENT_ANV = "application/x-anv";
        final String CONTENT_ASA = "text/asa";
        final String CONTENT_ASF = "video/x-ms-asf";
        final String CONTENT_ASP = "text/asp";
        final String CONTENT_ASX = "video/x-ms-asf";
        final String CONTENT_AU = "audio/basic";
        final String CONTENT_AVI = "video/avi";
        final String CONTENT_AWF = "application/vnd.adobe.workflow";
        final String CONTENT_BIZ = "text/xml";
        final String CONTENT_BMP = "application/x-bmp";
        final String CONTENT_BOT = "application/x-bot";
        final String CONTENT_C4T = "application/x-c4t";
        final String CONTENT_C90 = "application/x-c90";
        final String CONTENT_CAL = "application/x-cals";
        final String CONTENT_CAT = "application/vnd.ms-pki.seccat";
        final String CONTENT_CDF = "application/x-netcdf";
        final String CONTENT_CDR = "application/x-cdr";
        final String CONTENT_CEL = "application/x-cel";
        final String CONTENT_CER = "application/x-x509-ca-cert";
        final String CONTENT_CG4 = "application/x-g4";
        final String CONTENT_CGM = "application/x-cgm";
        final String CONTENT_CIT = "application/x-cit";
        final String CONTENT_CLASS = "java/*";
        final String CONTENT_CML = "text/xml";
        final String CONTENT_CMP = "application/x-cmp";
        final String CONTENT_CMX = "application/x-cmx";
        final String CONTENT_COT = "application/x-cot";
        final String CONTENT_CRL = "application/pkix-crl";
        final String CONTENT_CRT = "application/x-x509-ca-cert";
        final String CONTENT_CSI = "application/x-csi";
        final String CONTENT_CSS = "text/css";
        final String CONTENT_CUT = "application/x-cut";
        final String CONTENT_DBF = "application/x-dbf";
        final String CONTENT_DBM = "application/x-dbm";
        final String CONTENT_DBX = "application/x-dbx";
        final String CONTENT_DCD = "text/xml";
        final String CONTENT_DCX = "application/x-dcx";
        final String CONTENT_DER = "application/x-x509-ca-cert";
        final String CONTENT_DGN = "application/x-dgn";
        final String CONTENT_DIB = "application/x-dib";
        final String CONTENT_DLL = "application/x-msdownload";
        final String CONTENT_DOC = "application/msword";
        final String CONTENT_DOT = "application/msword";
        final String CONTENT_DRW = "application/x-drw";
        final String CONTENT_DTD = "application/xml-dtd";
        final String CONTENT_DWF = "application/x-dwf";
        final String CONTENT_DWG = "application/x-dwg";
        final String CONTENT_DXB = "application/x-dxb";
        final String CONTENT_DXF = "application/x-dxf";
        final String CONTENT_EDN = "application/vnd.adobe.edn";
        final String CONTENT_EMF = "application/x-emf";
        final String CONTENT_EML = "message/rfc822";
        final String CONTENT_ENT = "text/xml";
        final String CONTENT_EPI = "application/x-epi";
        final String CONTENT_EPS = "application/postscript";
        final String CONTENT_ETD = "application/x-ebx";
        final String CONTENT_EXE = "application/x-msdownload";
        final String CONTENT_FAX = "image/fax";
        final String CONTENT_FDF = "application/vnd.fdf";
        final String CONTENT_FIF = "application/fractals";
        final String CONTENT_FO = "text/xml";
        final String CONTENT_FRM = "application/x-frm";
        final String CONTENT_G4 = "application/x-g4";
        final String CONTENT_GBR = "application/x-gbr";
        final String CONTENT_GIF = "image/gif";
        final String CONTENT_GL2 = "application/x-gl2";
        final String CONTENT_GP4 = "application/x-gp4";
        final String CONTENT_HGL = "application/x-hgl";
        final String CONTENT_HMR = "application/x-hmr";
        final String CONTENT_HPG = "application/x-hpgl";
        final String CONTENT_HPL = "application/x-hpl";
        final String CONTENT_HQX = "application/mac-binhex40";
        final String CONTENT_HRF = "application/x-hrf";
        final String CONTENT_HTA = "application/hta";
        final String CONTENT_HTC = "text/x-component";
        final String CONTENT_HTM = "text/html";
        final String CONTENT_HTML = "text/html";
        final String CONTENT_HTT = "text/webviewhtml";
        final String CONTENT_HTX = "text/html";
        final String CONTENT_ICB = "application/x-icb";
        final String CONTENT_ICO = "application/x-ico";
        final String CONTENT_IFF = "application/x-iff";
        final String CONTENT_IG4 = "application/x-g4";
        final String CONTENT_IGS = "application/x-igs";
        final String CONTENT_III = "application/x-iphone";
        final String CONTENT_IMG = "application/x-img";
        final String CONTENT_INS = "application/x-internet-signup";
        final String CONTENT_ISP = "application/x-internet-signup";
        final String CONTENT_IVF = "video/x-ivf";
        final String CONTENT_JAVA = "java/*";
        final String CONTENT_JFIF = "image/jpeg";
        final String CONTENT_JPE = "application/x-jpe";
        final String CONTENT_JPEG = "image/jpeg";
        final String CONTENT_JPG = "application/x-jpg";
        final String CONTENT_JS = "application/x-javascript";
        final String CONTENT_JSP = "text/html";
        final String CONTENT_LA1 = "audio/x-liquid-file";
        final String CONTENT_LAR = "application/x-laplayer-reg";
        final String CONTENT_LATEX = "application/x-latex";
        final String CONTENT_LAVS = "audio/x-liquid-secure";
        final String CONTENT_LBM = "application/x-lbm";
        final String CONTENT_LMSFF = "audio/x-la-lms";
        final String CONTENT_LS = "application/x-javascript";
        final String CONTENT_LTR = "application/x-ltr";
        final String CONTENT_M1V = "video/x-mpeg";
        final String CONTENT_M2V = "video/x-mpeg";
        final String CONTENT_M3U = "audio/mpegurl";
        final String CONTENT_M4E = "video/mpeg4";
        final String CONTENT_MAC = "application/x-mac";
        final String CONTENT_MAN = "application/x-troff-man";
        final String CONTENT_MATH = "text/xml";
        final String CONTENT_MDB = "application/x-mdb";
        final String CONTENT_MFP = "application/x-shockwave-flash";
        final String CONTENT_MHT = "message/rfc822";
        final String CONTENT_MHTML = "message/rfc822";
        final String CONTENT_MI = "application/x-mi";
        final String CONTENT_MID = "audio/mid";
        final String CONTENT_MIDI = "audio/mid";
        final String CONTENT_MIL = "application/x-mil";
        final String CONTENT_MML = "text/xml";
        final String CONTENT_MND = "audio/x-musicnet-download";
        final String CONTENT_MNS = "audio/x-musicnet-stream";
        final String CONTENT_MOCHA = "application/x-javascript";
        final String CONTENT_MOVIE = "video/x-sgi-movie";
        final String CONTENT_MP1 = "audio/mp1";
        final String CONTENT_MP2 = "audio/mp2";
        final String CONTENT_MP2V = "video/mpeg";
        final String CONTENT_MP3 = "audio/mp3";
        final String CONTENT_MP4 = "video/mpeg4";
        final String CONTENT_MPA = "video/x-mpg";
        final String CONTENT_MPD = "application/vnd.ms-project";
        final String CONTENT_MPE = "video/x-mpeg";
        final String CONTENT_MPEG = "video/mpg";
        final String CONTENT_MPG = "video/mpg";
        final String CONTENT_MPGA = "audio/rn-mpeg";
        final String CONTENT_MPP = "application/vnd.ms-project";
        final String CONTENT_MPS = "video/x-mpeg";
        final String CONTENT_MPT = "application/vnd.ms-project";
        final String CONTENT_MPV = "video/mpg";
        final String CONTENT_MPV2 = "video/mpeg";
        final String CONTENT_MPW = "application/vnd.ms-project";
        final String CONTENT_MPX = "application/vnd.ms-project";
        final String CONTENT_MTX = "text/xml";
        final String CONTENT_MXP = "application/x-mmxp";
        final String CONTENT_NRF = "application/x-nrf";
        final String CONTENT_NWS = "message/rfc822";
        final String CONTENT_ODC = "text/x-ms-odc";
        final String CONTENT_OUT = "application/x-out";
        final String CONTENT_P10 = "application/pkcs10";
        final String CONTENT_P12 = "application/x-pkcs12";
        final String CONTENT_PCI = "application/x-pci";
        final String CONTENT_PDF = "application/pdf";
        final String CONTENT_PDX = "application/vnd.adobe.pdx";
        final String CONTENT_PNG = "application/x-png";
        final String CONTENT_POT = "application/vnd.ms-powerpoint";
        final String CONTENT_PPT = "application/x-ppt";
        final String CONTENT_PR = "application/x-pr";
        final String CONTENT_PRF = "application/pics-rules";
        final String CONTENT_PS = "application/postscript";
        final String CONTENT_RAT = "application/rat-file";
        final String CONTENT_RDF = "text/xml";
        final String CONTENT_RGB = "application/x-rgb";
        final String CONTENT_RM = "application/vnd.rn-realmedia";
        final String CONTENT_RMF = "application/vnd.adobe.rmf";
        final String CONTENT_SMIL = "application/smil";
        final String CONTENT_STL = "application/vnd.ms-pki.stl";
        final String CONTENT_SVG = "image/svg+xml";
        final String CONTENT_TSD = "text/xml";
        final String CONTENT_TXT = "text/plain";
        final String CONTENT_VSS = "application/vnd.visio";
        final String CONTENT_VST = "application/vnd.visio";
        final String CONTENT_WAV = "audio/wav";
        final String CONTENT_WB1 = "application/x-wb1";
        final String CONTENT_WS = "application/x-ws";
        final String CONTENT_WSDL = "text/xml";
        final String CONTENT_XHTML = "text/html";
        final String CONTENT_XLS = "application/x-xls";
        final String CONTENT_XLW = "application/x-xlw";
        final String CONTENT_XSL = "text/xml";
        final String CONTENT_IPA = "application/vnd.iphone";
        final String CONTENT_APK = "application/vnd.android.package-archive";
        final String CONTENT_XAP = "application/x-silverlight-app";
        final String CONTENT_Z = "application/x-compress";
        final String CONTENT_ZABW = "application/x-abiword";
        final String CONTENT_ZIP = "application/zip";
        final String CONTENT_ZOO = "application/x-zoo";
    }


    private String quoteIfNeeded(String value) {
        if (ContentTypeTool.isValidToken(value)) {
            return value;
        }
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }
    public static class ContentTypeTool{
        private static final Pattern TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9!#$%&'*+\\-.^_`|~]+$");

        public ContentTypeTool(){}
        private static boolean isValidToken(String s) {
            return TOKEN_PATTERN.matcher(s).matches();
        }
        public ContentType parse(String contentType) {
            if (contentType == null || contentType.isEmpty()) {
                throw new IllegalArgumentException("ContentType cannot be null or empty");
            }

            String[] parts = contentType.split(";", 2);
            String fullType = parts[0].trim();
            String[] typeParts = fullType.split("/", 2);
            if (typeParts.length != 2) {
                throw new IllegalArgumentException("Invalid content type format: " + contentType);
            }

            ContentType result = new ContentType(typeParts[0].trim(), typeParts[1].trim());

            if (parts.length > 1) {
                parseParameters(parts[1], result.parameters);
            }
            return result;

        }

        private void parseParameters(String paramsString, Map<String, String> parameters) {
            String[] params = paramsString.split(";");
            for (String param : params) {
                param = param.trim();
                if (param.isEmpty()) continue;

                String[] keyValue = param.split("=", 2);
                if (keyValue.length != 2) continue;

                String key = keyValue[0].trim().toLowerCase();
                String value = keyValue[1].trim();

                // Remove surrounding quotes and handle escaped characters
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = unquote(value);
                }

                parameters.put(key, value);
            }
        }
        private String unquote(String value) {
            return value.substring(1, value.length() - 1)
                    .replaceAll("\\\\(.)", "$1");
        }
    }
    public ContentType(String type, String subtype) {
        if (!ContentTypeTool.isValidToken(type)) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        if (!ContentTypeTool.isValidToken(subtype)) {
            throw new IllegalArgumentException("Invalid subtype: " + subtype);
        }
        this.type = type;
        this.subtype = subtype;
        this.parameters = new LinkedHashMap<>();
    }

    public ContentType setCharset(String charset) {
        if (charset == null || charset.isEmpty()) {
            parameters.remove("charset");
        } else {
            parameters.put("charset", charset);
        }
        return this;
    }

    public String getCharset() {
        return parameters.get("charset");
    }

    public ContentType addParameter(String key, String value) {
        parameters.put(key.toLowerCase(), value);
        return this;
    }

    public ContentType removeParameter(String key) {
        parameters.remove(key.toLowerCase());
        return this;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append('/').append(subtype);

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            sb.append("; ").append(entry.getKey()).append('=').append(quoteIfNeeded(entry.getValue()));
        }
        return sb.toString();
    }
}
