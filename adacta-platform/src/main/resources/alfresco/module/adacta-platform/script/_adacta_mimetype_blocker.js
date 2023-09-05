/**
 * Inbound rule script for blocking all mimetypes except PDF.
 */
if (document && document.properties.content.mimetype != "application/pdf") {
    throw "Unsupported file format";
}