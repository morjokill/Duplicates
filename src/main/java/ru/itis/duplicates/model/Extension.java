package ru.itis.duplicates.model;

import ru.itis.duplicates.util.Utils;

import java.io.IOException;
import java.io.InputStream;

public enum Extension {
    PLAIN("") {
        @Override
        public String getText(InputStream inputStream) throws IOException {
            return Utils.getStringFromIS(inputStream);
        }
    },
    TXT("txt") {
        @Override
        public String getText(InputStream inputStream) throws IOException {
            return Utils.getStringFromIS(inputStream);
        }
    },
    DOC("doc") {
        @Override
        public String getText(InputStream inputStream) throws IOException {
            return Utils.getStringFromDocIS(inputStream);
        }
    },
    DOCX("docx") {
        @Override
        public String getText(InputStream inputStream) throws IOException {
            return Utils.getStringFromDocIS(inputStream);
        }
    },
    PDF("pdf") {
        @Override
        public String getText(InputStream inputStream) throws IOException {
            return Utils.getStringFromPdfIS(inputStream);
        }
    };

    private String extension;

    Extension(String extension) {
        this.extension = extension;
    }

    public abstract String getText(InputStream inputStream) throws IOException;

    public static Extension getExtension(String fileName) {
        String extension = Utils.getFileExtension(fileName);

        Extension[] values = Extension.values();
        for (Extension value : values) {
            if (extension.equals(value.extension)) {
                return value;
            }
        }
        return null;
    }
}
