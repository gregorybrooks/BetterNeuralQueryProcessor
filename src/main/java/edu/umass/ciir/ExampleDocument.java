package edu.umass.ciir;

import java.util.List;

public class ExampleDocument {
    private String docid;
    private String docText;
    private String highlight;
    private List<String> sentences;

    ExampleDocument(String docid, String docText, String highlight, List<String> sentences) {
        this.highlight = highlight;
        this.docid = docid;
        this.docText = docText;
        this.sentences = sentences;
    }

    ExampleDocument(ExampleDocument other) {
        this.docid = other.docid;
        this.docText = other.docText;
        this.highlight = other.highlight;
        this.sentences = other.sentences;
    }

    public String getDocid() {
        return docid;
    }

    public String getDocText() {
        return docText;
    }

    public List<String> getSentences() {
        return sentences;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public void setDocText(String docText) {
        this.docText = docText;
    }

}

