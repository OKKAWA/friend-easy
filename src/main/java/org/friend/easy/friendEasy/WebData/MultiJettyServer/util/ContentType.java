package org.friend.easy.friendEasy.WebData.MultiJettyServer.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 表示和解析HTTP Content-Type头部字段。
 * 包含类型(type)、子类型(subtype)和参数(parameters)，例如："text/html; charset=UTF-8"。
 */
public class ContentType {
    // 主类型，如"text"、"application"
    private final String type;
    // 子类型，如"html"、"json"
    private final String subtype;
    // 参数键值对，保持插入顺序，如charset=UTF-8
    private final Map<String, String> parameters;

    /**
     * 预定义常用Content-Type常量的接口。
     * 包含如"application/json"、"text/html"等标准MIME类型。
     */
    public interface SimpleContentType {
        String CONTENT_JSON = "application/json";
        String CONTENT_STAR = "application/octet-stream";
        String CONTENT_TIF = "image/tiff";
        String CONTENT_X001 = "application/x-001";
        String CONTENT_X301 = "application/x-301";
        String CONTENT_X323 = "text/h323";
        String CONTENT_X906 = "application/x-906";
        String CONTENT_X907 = "drawing/907";
        String CONTENT_A11 = "application/x-a11";
        String CONTENT_ACP = "audio/x-mei-aac";
        String CONTENT_AI = "application/postscript";
        String CONTENT_AIF = "audio/aiff";
        String CONTENT_AIFC = "audio/aiff";
        String CONTENT_AIFF = "audio/aiff";
        String CONTENT_ANV = "application/x-anv";
        String CONTENT_ASA = "text/asa";
        String CONTENT_ASF = "video/x-ms-asf";
        String CONTENT_ASP = "text/asp";
        String CONTENT_ASX = "video/x-ms-asf";
        String CONTENT_AU = "audio/basic";
        String CONTENT_AVI = "video/avi";
        String CONTENT_AWF = "application/vnd.adobe.workflow";
        String CONTENT_BIZ = "text/xml";
        String CONTENT_BMP = "application/x-bmp";
        String CONTENT_BOT = "application/x-bot";
        String CONTENT_C4T = "application/x-c4t";
        String CONTENT_C90 = "application/x-c90";
        String CONTENT_CAL = "application/x-cals";
        String CONTENT_CAT = "application/vnd.ms-pki.seccat";
        String CONTENT_CDF = "application/x-netcdf";
        String CONTENT_CDR = "application/x-cdr";
        String CONTENT_CEL = "application/x-cel";
        String CONTENT_CER = "application/x-x509-ca-cert";
        String CONTENT_CG4 = "application/x-g4";
        String CONTENT_CGM = "application/x-cgm";
        String CONTENT_CIT = "application/x-cit";
        String CONTENT_CLASS = "java/*";
        String CONTENT_CML = "text/xml";
        String CONTENT_CMP = "application/x-cmp";
        String CONTENT_CMX = "application/x-cmx";
        String CONTENT_COT = "application/x-cot";
        String CONTENT_CRL = "application/pkix-crl";
        String CONTENT_CRT = "application/x-x509-ca-cert";
        String CONTENT_CSI = "application/x-csi";
        String CONTENT_CSS = "text/css";
        String CONTENT_CUT = "application/x-cut";
        String CONTENT_DBF = "application/x-dbf";
        String CONTENT_DBM = "application/x-dbm";
        String CONTENT_DBX = "application/x-dbx";
        String CONTENT_DCD = "text/xml";
        String CONTENT_DCX = "application/x-dcx";
        String CONTENT_DER = "application/x-x509-ca-cert";
        String CONTENT_DGN = "application/x-dgn";
        String CONTENT_DIB = "application/x-dib";
        String CONTENT_DLL = "application/x-msdownload";
        String CONTENT_DOC = "application/msword";
        String CONTENT_DOT = "application/msword";
        String CONTENT_DRW = "application/x-drw";
        String CONTENT_DTD = "application/xml-dtd";
        String CONTENT_DWF = "application/x-dwf";
        String CONTENT_DWG = "application/x-dwg";
        String CONTENT_DXB = "application/x-dxb";
        String CONTENT_DXF = "application/x-dxf";
        String CONTENT_EDN = "application/vnd.adobe.edn";
        String CONTENT_EMF = "application/x-emf";
        String CONTENT_EML = "message/rfc822";
        String CONTENT_ENT = "text/xml";
        String CONTENT_EPI = "application/x-epi";
        String CONTENT_EPS = "application/postscript";
        String CONTENT_ETD = "application/x-ebx";
        String CONTENT_EXE = "application/x-msdownload";
        String CONTENT_FAX = "image/fax";
        String CONTENT_FDF = "application/vnd.fdf";
        String CONTENT_FIF = "application/fractals";
        String CONTENT_FO = "text/xml";
        String CONTENT_FRM = "application/x-frm";
        String CONTENT_G4 = "application/x-g4";
        String CONTENT_GBR = "application/x-gbr";
        String CONTENT_GIF = "image/gif";
        String CONTENT_GL2 = "application/x-gl2";
        String CONTENT_GP4 = "application/x-gp4";
        String CONTENT_HGL = "application/x-hgl";
        String CONTENT_HMR = "application/x-hmr";
        String CONTENT_HPG = "application/x-hpgl";
        String CONTENT_HPL = "application/x-hpl";
        String CONTENT_HQX = "application/mac-binhex40";
        String CONTENT_HRF = "application/x-hrf";
        String CONTENT_HTA = "application/hta";
        String CONTENT_HTC = "text/x-component";
        String CONTENT_HTM = "text/html";
        String CONTENT_HTML = "text/html";
        String CONTENT_HTT = "text/webviewhtml";
        String CONTENT_HTX = "text/html";
        String CONTENT_ICB = "application/x-icb";
        String CONTENT_ICO = "application/x-ico";
        String CONTENT_IFF = "application/x-iff";
        String CONTENT_IG4 = "application/x-g4";
        String CONTENT_IGS = "application/x-igs";
        String CONTENT_III = "application/x-iphone";
        String CONTENT_IMG = "application/x-img";
        String CONTENT_INS = "application/x-internet-signup";
        String CONTENT_ISP = "application/x-internet-signup";
        String CONTENT_IVF = "video/x-ivf";
        String CONTENT_JAVA = "java/*";
        String CONTENT_JFIF = "image/jpeg";
        String CONTENT_JPE = "application/x-jpe";
        String CONTENT_JPEG = "image/jpeg";
        String CONTENT_JPG = "application/x-jpg";
        String CONTENT_JS = "application/x-javascript";
        String CONTENT_JSP = "text/html";
        String CONTENT_LA1 = "audio/x-liquid-file";
        String CONTENT_LAR = "application/x-laplayer-reg";
        String CONTENT_LATEX = "application/x-latex";
        String CONTENT_LAVS = "audio/x-liquid-secure";
        String CONTENT_LBM = "application/x-lbm";
        String CONTENT_LMSFF = "audio/x-la-lms";
        String CONTENT_LS = "application/x-javascript";
        String CONTENT_LTR = "application/x-ltr";
        String CONTENT_M1V = "video/x-mpeg";
        String CONTENT_M2V = "video/x-mpeg";
        String CONTENT_M3U = "audio/mpegurl";
        String CONTENT_M4E = "video/mpeg4";
        String CONTENT_MAC = "application/x-mac";
        String CONTENT_MAN = "application/x-troff-man";
        String CONTENT_MATH = "text/xml";
        String CONTENT_MDB = "application/x-mdb";
        String CONTENT_MFP = "application/x-shockwave-flash";
        String CONTENT_MHT = "message/rfc822";
        String CONTENT_MHTML = "message/rfc822";
        String CONTENT_MI = "application/x-mi";
        String CONTENT_MID = "audio/mid";
        String CONTENT_MIDI = "audio/mid";
        String CONTENT_MIL = "application/x-mil";
        String CONTENT_MML = "text/xml";
        String CONTENT_MND = "audio/x-musicnet-download";
        String CONTENT_MNS = "audio/x-musicnet-stream";
        String CONTENT_MOCHA = "application/x-javascript";
        String CONTENT_MOVIE = "video/x-sgi-movie";
        String CONTENT_MP1 = "audio/mp1";
        String CONTENT_MP2 = "audio/mp2";
        String CONTENT_MP2V = "video/mpeg";
        String CONTENT_MP3 = "audio/mp3";
        String CONTENT_MP4 = "video/mpeg4";
        String CONTENT_MPA = "video/x-mpg";
        String CONTENT_MPD = "application/vnd.ms-project";
        String CONTENT_MPE = "video/x-mpeg";
        String CONTENT_MPEG = "video/mpg";
        String CONTENT_MPG = "video/mpg";
        String CONTENT_MPGA = "audio/rn-mpeg";
        String CONTENT_MPP = "application/vnd.ms-project";
        String CONTENT_MPS = "video/x-mpeg";
        String CONTENT_MPT = "application/vnd.ms-project";
        String CONTENT_MPV = "video/mpg";
        String CONTENT_MPV2 = "video/mpeg";
        String CONTENT_MPW = "application/vnd.ms-project";
        String CONTENT_MPX = "application/vnd.ms-project";
        String CONTENT_MTX = "text/xml";
        String CONTENT_MXP = "application/x-mmxp";
        String CONTENT_NRF = "application/x-nrf";
        String CONTENT_NWS = "message/rfc822";
        String CONTENT_ODC = "text/x-ms-odc";
        String CONTENT_OUT = "application/x-out";
        String CONTENT_P10 = "application/pkcs10";
        String CONTENT_P12 = "application/x-pkcs12";
        String CONTENT_PCI = "application/x-pci";
        String CONTENT_PDF = "application/pdf";
        String CONTENT_PDX = "application/vnd.adobe.pdx";
        String CONTENT_PNG = "application/x-png";
        String CONTENT_POT = "application/vnd.ms-powerpoint";
        String CONTENT_PPT = "application/x-ppt";
        String CONTENT_PR = "application/x-pr";
        String CONTENT_PRF = "application/pics-rules";
        String CONTENT_PS = "application/postscript";
        String CONTENT_RAT = "application/rat-file";
        String CONTENT_RDF = "text/xml";
        String CONTENT_RGB = "application/x-rgb";
        String CONTENT_RM = "application/vnd.rn-realmedia";
        String CONTENT_RMF = "application/vnd.adobe.rmf";
        String CONTENT_SMIL = "application/smil";
        String CONTENT_STL = "application/vnd.ms-pki.stl";
        String CONTENT_SVG = "image/svg+xml";
        String CONTENT_TSD = "text/xml";
        String CONTENT_TXT = "text/plain";
        String CONTENT_VSS = "application/vnd.visio";
        String CONTENT_VST = "application/vnd.visio";
        String CONTENT_WAV = "audio/wav";
        String CONTENT_WB1 = "application/x-wb1";
        String CONTENT_WS = "application/x-ws";
        String CONTENT_WSDL = "text/xml";
        String CONTENT_XHTML = "text/html";
        String CONTENT_XLS = "application/x-xls";
        String CONTENT_XLW = "application/x-xlw";
        String CONTENT_XSL = "text/xml";
        String CONTENT_IPA = "application/vnd.iphone";
        String CONTENT_APK = "application/vnd.android.package-archive";
        String CONTENT_XAP = "application/x-silverlight-app";
        String CONTENT_Z = "application/x-compress";
        String CONTENT_ZABW = "application/x-abiword";
        String CONTENT_ZIP = "application/zip";
        String CONTENT_ZOO = "application/x-zoo";
    }

    /**
     * 根据参数值判断是否需要添加引号，并处理转义。
     * @param value 参数值
     * @return 若值符合token规范则返回原值，否则返回带转义引号的字符串
     */
    private String quoteIfNeeded(String value) {
        if (ContentTypeTool.isValidToken(value)) {
            return value;
        }
        // 转义双引号并添加外围引号
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    /**
     * ContentType解析工具类，包含验证和解析逻辑。
     */
    public static class ContentTypeTool {
        // 符合RFC规范的token正则表达式
        private static Pattern TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9!#$%&'*+\\-.^_`|~]+$");

        /**
         * 验证字符串是否符合token规范。
         * @param s 待验证字符串
         * @return 验证结果
         */
        private static boolean isValidToken(String s) {
            return TOKEN_PATTERN.matcher(s).matches();
        }

        /**
         * 解析Content-Type字符串为ContentType对象。
         * @param contentType 格式如"type/subtype; param1=value1"
         * @return 解析后的ContentType对象
         * @throws IllegalArgumentException 当输入格式无效时抛出
         */
        public ContentType parse(String contentType) {
            // 验证输入非空
            if (contentType == null || contentType.isEmpty()) {
                throw new IllegalArgumentException("ContentType cannot be null or empty");
            }

            // 分割主类型和参数部分
            String[] parts = contentType.split(";", 2);
            String fullType = parts[0].trim();
            String[] typeParts = fullType.split("/", 2);
            if (typeParts.length != 2) {
                throw new IllegalArgumentException("Invalid content type format: " + contentType);
            }

            // 创建ContentType实例
            ContentType result = new ContentType(typeParts[0].trim(), typeParts[1].trim());

            // 解析参数
            if (parts.length > 1) {
                parseParameters(parts[1], result.parameters);
            }
            return result;
        }

        /**
         * 解析参数字符串到参数Map。
         * @param paramsString 参数字符串，如"charset=UTF-8; version=1.0"
         * @param parameters 参数存储的Map
         */
        private void parseParameters(String paramsString, Map<String, String> parameters) {
            String[] params = paramsString.split(";");
            for (String param : params) {
                param = param.trim();
                if (param.isEmpty()) continue;

                // 分割键值对
                String[] keyValue = param.split("=", 2);
                if (keyValue.length != 2) continue;

                String key = keyValue[0].trim().toLowerCase(); // 键转为小写
                String value = keyValue[1].trim();

                // 处理带引号的值
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = unquote(value);
                }

                parameters.put(key, value);
            }
        }

        /**
         * 去除值的外围引号并处理转义字符。
         * @param value 带引号的字符串
         * @return 处理后的纯字符串
         */
        private String unquote(String value) {
            return value.substring(1, value.length() - 1)
                    .replaceAll("\\\\(.)", "$1"); // 处理转义字符，如\"转为"
        }
    }

    /**
     * 构造ContentType实例。
     * @param type 主类型，必须符合token规范
     * @param subtype 子类型，必须符合token规范
     * @throws IllegalArgumentException 当类型或子类型无效时抛出
     */
    public ContentType(String type, String subtype) {
        // 验证类型和子类型的有效性
        if (!ContentTypeTool.isValidToken(type)) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        if (!ContentTypeTool.isValidToken(subtype)) {
            throw new IllegalArgumentException("Invalid subtype: " + subtype);
        }
        this.type = type;
        this.subtype = subtype;
        this.parameters = new LinkedHashMap<>(); // 保持参数顺序
    }

    /**
     * 设置字符集参数。
     * @param charset 字符集名称，如"UTF-8"。传空或null则移除该参数
     * @return 当前对象，支持链式调用
     */
    public ContentType setCharset(String charset) {
        if (charset == null || charset.isEmpty()) {
            parameters.remove("charset");
        } else {
            parameters.put("charset", charset);
        }
        return this;
    }

    /**
     * 获取当前字符集参数值。
     * @return 字符集字符串，若不存在返回null
     */
    public String getCharset() {
        return parameters.get("charset");
    }

    /**
     * 添加或更新参数。
     * @param key 参数键（自动转为小写）
     * @param value 参数值
     * @return 当前对象，支持链式调用
     */
    public ContentType addParameter(String key, String value) {
        parameters.put(key.toLowerCase(), value);
        return this;
    }

    /**
     * 移除指定参数。
     * @param key 参数键（不区分大小写）
     * @return 当前对象，支持链式调用
     */
    public ContentType removeParameter(String key) {
        parameters.remove(key.toLowerCase());
        return this;
    }

    // 以下为getter方法
    public String getType() { return type; }
    public String getSubtype() { return subtype; }

    /**
     * 生成符合规范的Content-Type字符串。
     * @return 如"text/html; charset=UTF-8"
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append('/').append(subtype);

        // 拼接参数，自动处理引号
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            sb.append("; ")
                    .append(entry.getKey())
                    .append('=')
                    .append(quoteIfNeeded(entry.getValue()));
        }
        return sb.toString();
    }
}