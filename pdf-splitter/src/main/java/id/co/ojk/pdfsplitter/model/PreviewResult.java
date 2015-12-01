package id.co.ojk.pdfsplitter.model;

public class PreviewResult extends ResponseResult{
  private String pdfName;
  private String[] imagesUrl;

  public String getPdfName(){
    return pdfName;
  }

  public void setPdfName(String pdfName){
    this.pdfName = pdfName;
  }
  
  public String[] getImagesUrl(){
    return imagesUrl;
  }

  public void setImagesUrl(String[] imagesUrl){
    this.imagesUrl = imagesUrl;
  }
}
