package id.co.ojk.pdfsplitter.model;

public class ResponseResult{
  private int success = -1;
  private String errMsg;
  
  public int getSuccess(){
    return success;
  }

  public void setSuccess(int success){
    this.success = success;
  }
  
  public String getErrMsg(){
    return errMsg;
  }

  public void setErrMsg(String errMsg){
    this.errMsg = errMsg;
  }
}
