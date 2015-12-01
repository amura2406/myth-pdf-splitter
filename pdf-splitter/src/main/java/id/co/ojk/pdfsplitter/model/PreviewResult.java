package id.co.ojk.pdfsplitter.model;

public class PreviewResult{
  private int success = -1;
  private String pdfName;
  private String errMsg;
  
  private String[] imagesUrl;

  public int getSuccess(){
    return success;
  }

  public void setSuccess(int success){
    this.success = success;
  }

  public String getPdfName(){
    return pdfName;
  }

  public void setPdfName(String pdfName){
    this.pdfName = pdfName;
  }
  
  public String getErrMsg(){
    return errMsg;
  }

  public void setErrMsg(String errMsg){
    this.errMsg = errMsg;
  }
  
  public String[] getImagesUrl(){
    return imagesUrl;
  }

  public void setImagesUrl(String[] imagesUrl){
    this.imagesUrl = imagesUrl;
  }
}
