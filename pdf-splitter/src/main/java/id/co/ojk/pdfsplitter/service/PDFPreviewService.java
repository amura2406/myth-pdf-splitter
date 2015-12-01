package id.co.ojk.pdfsplitter.service;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.imgscalr.Scalr.*;

@Service
public class PDFPreviewService{
  private static final Logger LOGGER = LoggerFactory.getLogger(PDFPreviewService.class);
  private static final String IMG_FORMAT = "jpg";
  
  @Value("${result.folder}")
  private String baseResultFolder;
  @Value("${images.subpath}")
  private String subPath;
  @Value("${resized.width}")
  private int rW;
  
  @SuppressWarnings("unchecked")
  public String[] getPreview(PDDocument document, String resultFolder) throws IllegalStateException, IOException{
    boolean bSuccess = true;
    List<PDPage> pages = document.getDocumentCatalog().getAllPages();
    int pagesSize = pages.size();
    String[] result = new String[pagesSize];
    
    int resolution;
    try{
      resolution = Toolkit.getDefaultToolkit().getScreenResolution();
    }catch(HeadlessException e){
      resolution = 96;
    }
    
    for(int i = 0; i < pagesSize; i++){
        PDPage page = pages.get(i);
        BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB, resolution);
        BufferedImage resizedImg = resize(image, Method.SPEED, Mode.FIT_TO_WIDTH, rW, OP_ANTIALIAS);
        
        int imgIdx = i+1;
        String fileName = String.format("%s%s%d.%s", resultFolder, File.separator, imgIdx, IMG_FORMAT);
        String relWebPath = String.format("/%s/%s/%d.%s", subPath, resultFolder, imgIdx, IMG_FORMAT);
        File testFilePath = new File(baseResultFolder + fileName);
        if(!testFilePath.exists()) testFilePath.getParentFile().mkdirs();
        LOGGER.debug("Writing: " + testFilePath.getAbsolutePath());
        bSuccess &= ImageIOUtil.writeImage(resizedImg, testFilePath.getAbsolutePath(), resolution);
        result[i] = relWebPath;
    }
    
    if(!bSuccess) throw new IllegalStateException("Can't dump PDF to images !");
    return result;
  }
}
