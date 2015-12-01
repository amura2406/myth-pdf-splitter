package id.co.ojk.pdfsplitter.controller;

import id.co.ojk.pdfsplitter.model.PreviewResult;
import id.co.ojk.pdfsplitter.service.PDFPreviewService;
import id.co.ojk.pdfsplitter.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/split")
public class SplitterController{
  private static final Logger LOGGER = LoggerFactory.getLogger(SplitterController.class);
  private static final String DF = "yyyyMMdd-HHmmss";
  private static final String PDF_FORMAT = ".pdf";
  
  @Value("${source.folder}")
  private String baseSourceFolder;
  
  @Value("${result.folder}")
  private String baseResultFolder;
  
  @Autowired
  private PDFPreviewService previewService;
  
  @RequestMapping(value="/upload", method=RequestMethod.POST)
  public @ResponseBody PreviewResult upload(@RequestParam("pdf") MultipartFile pdfFile){
    long startTs = System.currentTimeMillis();
    
    PreviewResult result = new PreviewResult();
    if(!pdfFile.isEmpty() && pdfFile.getOriginalFilename().toLowerCase().endsWith(PDF_FORMAT)){
      PDDocument document = null;
      try{
        Date now = new Date();
        
        DateFormat df = new SimpleDateFormat(DF);
        String md5Hex = DigestUtils.md5Hex("" + now.getTime());
        
        String folderName = String.format("%s-%s", df.format(now), md5Hex);
        
        File tempFile = new File(String.format("%s%s%sori%s", baseResultFolder, folderName, File.separator, PDF_FORMAT));
        LOGGER.info("Relocating PDF file to : {}", tempFile.getAbsolutePath());
        tempFile.getParentFile().mkdirs();
        pdfFile.transferTo(tempFile);
        
        document = PDDocument.loadNonSeq(tempFile, null);
        
        String[] previews = previewService.getPreview(document, folderName);
        result.setSuccess(1);
        result.setPdfName(folderName);
        result.setImagesUrl(previews);
        LOGGER.info("Processed previews in {} milliseconds", System.currentTimeMillis()-startTs);
      }catch(Exception e){
        LOGGER.error("Error while previewing document [{}] : {}", pdfFile.getName(), e.getMessage());
        result.setErrMsg(String.format("Error while previewing document [%s] : %s", pdfFile.getName(), e.getMessage()));
      }
    }else{
      LOGGER.info("Doesn't receive PDF file !");
      result.setErrMsg("Server doesn't receive PDF file !");
    }
    return result;
  }
  
  @RequestMapping(value="/preview/{relPath}/{pdfName:.*}", method=RequestMethod.GET)
  public @ResponseBody PreviewResult preview(@PathVariable String relPath, 
                                             @PathVariable String pdfName){
    long startTs = System.currentTimeMillis();
    
    PreviewResult result = new PreviewResult();
    File source = new File(String.format("%s%s%s%s%s", baseSourceFolder, File.separator, relPath, File.separator, pdfName));
    
    if(!source.exists()){
      PDDocument document = null;
      try{
        Date now = new Date();
        
        DateFormat df = new SimpleDateFormat(DF);
        String md5Hex = DigestUtils.md5Hex("" + now.getTime());
        
        String folderName = String.format("%s-%s", df.format(now), md5Hex);
        
        File tempFile = new File(String.format("%s%s%sori%s", baseResultFolder, folderName, File.separator, PDF_FORMAT));
        LOGGER.info("Relocating PDF file to : {}", tempFile.getAbsolutePath());
        tempFile.getParentFile().mkdirs();
        FileUtil.fastChannelCopy(new FileInputStream(source), new FileOutputStream(tempFile));
        
        document = PDDocument.loadNonSeq(tempFile, null);
        
        String[] previews = previewService.getPreview(document, folderName);
        result.setSuccess(1);
        result.setPdfName(folderName);
        result.setImagesUrl(previews);
        LOGGER.info("Processed previews in {} milliseconds", System.currentTimeMillis()-startTs);
      }catch(Exception e){
        LOGGER.error("Error while previewing document [{}] : {}", source.getName(), e.getMessage());
        result.setErrMsg(String.format("Error while previewing document [%s] : %s", source.getName(), e.getMessage()));
      }
    }else{
      LOGGER.info("Doesn't receive PDF file !");
      result.setErrMsg("Server doesn't receive PDF file !");
    }
    return result;
  }
}
