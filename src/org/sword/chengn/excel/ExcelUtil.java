/**
 * 基于poi操作excel的一个工具类
 * 项目地址 https://github.com/chengn/ExcelUtil 
 * 项目讨论区 https://github.com/chengn/ExcelUtil/issues 
 * 
 * 如果你使用了此工具，那么希望你也能为此工具贡献代码
 * 1 fork此项目
 * 2 如果修改了有意思的功能，你可以pull request来提交
 * 3 如果没有修改，有好的想法，你可以去项目的讨论区
 *   https://github.com/chengn/ExcelUtil/issues来提出想法
 */
package org.sword.chengn.excel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Cell;


/**
 * Excel 操作工具
 * @author ChengNing
 * @date   2014-9-5
 */
public class ExcelUtil {
	private static Logger logger = Logger.getLogger(ExcelUtil.class);
	
	public static final String ABSOLUTE_CHAR = "$";
	public static final String RANGE_CHAR = ":";

	/**
	 * copy row
	 * @param source
	 * @param target
	 * @param cellCount
	 */
	public static void copyRow(HSSFRow source,HSSFRow target){
		if(target == null)
			return;
		target.setHeight(source.getHeight());
		int cellCount = source.getLastCellNum();
		for(int i=0;i<cellCount;i++){
			HSSFCell sourceCell = source.getCell(i);
			HSSFCell targetCell = target.createCell(i);
			if(sourceCell != null && targetCell != null){
				copyCell(sourceCell, targetCell);
			}
		}
	}
	
	/**
	 * copy cell
	 * @param source
	 * @param target
	 */
	public static void copyCell(HSSFCell source,HSSFCell target){
		if(target == null)
			return;

        if (source.getCellComment() != null) {  
        	target.setCellComment(source.getCellComment());  
        }  
        target.setCellStyle(source.getCellStyle());
		target.setCellType(source.getCellType());
		
		copyValue(source, target);
		
		copyFormula(source, target);
		
	}
	
	/**
	 * 
	 * @param source
	 * @param target
	 */
	public static void copyValue(HSSFCell source,HSSFCell target){
		int cellType = source.getCellType();
		switch (cellType) {
		case Cell.CELL_TYPE_STRING:
			target.setCellValue(source.getStringCellValue());
			break;
		case Cell.CELL_TYPE_BOOLEAN:
			target.setCellValue(source.getBooleanCellValue());
			break;
		case Cell.CELL_TYPE_NUMERIC:
            if (HSSFDateUtil.isCellDateFormatted(source)) {  
            	target.setCellValue(source.getDateCellValue());  
            } else {  
            	target.setCellValue(source.getNumericCellValue());  
            }  
			target.setCellValue(source.getNumericCellValue());
			break;
		case Cell.CELL_TYPE_ERROR:
			target.setCellErrorValue(source.getErrorCellValue());
			break;
		case Cell.CELL_TYPE_BLANK:
			//do nothing
			break;
		case Cell.CELL_TYPE_FORMULA:
			target.setCellFormula(source.getCellFormula());
			break;
		default:
			logger.error("类型为" + source.getCellType() + "无法复制");
			break;
		}
	}
	
	/**
	 * 复制公式，同excel的复制，目前只支持行
	 * @param source
	 * @param target
	 */
	public static void copyFormula(HSSFCell source,HSSFCell target){
		if(source.getCellType() != Cell.CELL_TYPE_FORMULA)
			return;
		String formula = source.getCellFormula();
		if(StringUtils.isBlank(formula))
			return;
		int sRowIndex = source.getRowIndex();
		int tRowIndex = target.getRowIndex();
		int moveCount = tRowIndex - sRowIndex;
		formula = moveFormula(formula, moveCount);
		
		target.setCellFormula(formula);
	}
	
	/**
	 * 移动行公式
	 * @param formula    移动之前的公式
	 * @param moveRowCount  移动的行数
	 * @return   移动之后的公式
	 */
	public static String moveFormula(String formula,int moveRowCount){
		return moveFormula(formula,moveRowCount,0);
	}
	
	/**
	 * 更新cell中的公式
	 * @param cell         需要更新公式的cell
	 * @param insertRowCount   插入行的总数，引起公式更新的原因
	 * @param insertPoint      插入点，涉及到excel公式更新的算法
	 */
	public static void updateFormula(HSSFCell cell,int insertRowCount,int insertPoint){
		if(cell.getCellType() != Cell.CELL_TYPE_FORMULA)
			return;
		String formula = cell.getCellFormula();
		if(StringUtils.isBlank(formula))
			return;
		formula = moveFormula(formula, insertRowCount, insertPoint);
		cell.setCellFormula(formula);
	}

	/**
	 * 移动行公式
	 * @param formula   移动之前的公式
	 * @param moveRowCount 移动的行数
	 * @param insertRowNum 导致移动的行号,Excel中的行号，下标从0开始，公式中比此数大的行才更新，如果比此行小，则不用更新
	 * @return
	 */
	public static String moveFormula(String formula,int moveRowCount,int insertRowNum){
		if(moveRowCount == 0){
			return formula;
		}
		List<String> sRowNumList = new ArrayList<String>();
		List<String> tRowNumList = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		char[] chs = formula.toCharArray();
		boolean isStable = false; //固定符号$标识
		String numStr = "";
		for(char c : chs){
			String s = String.valueOf(c);
			if(StringUtils.isNumeric(s)){
				numStr = numStr + s;
				continue;  //组合数字，知道下一位不是数字或者结束，标识一个完整的数据
			}
			else if(StringUtils.equals(s, ABSOLUTE_CHAR)){
				isStable = true;
			}
			//numStr不是空（针对多位数），表示一个完整数据
			if(!StringUtils.isBlank(numStr)){
				if(isStable){
					sb.append(numStr);
					isStable = false;
				}
				else{
				    sb.append(calFormulaNum(numStr,moveRowCount,insertRowNum));
				}
				numStr = "";//及时清空
			}
			sb.append(s);
		}
		//防止最后是数字直接结束
		if(!StringUtils.isBlank(numStr)){
			if(isStable){
				sb.append(numStr);
			}
			else{
			    sb.append(calFormulaNum(numStr,moveRowCount,insertRowNum));
			}
			numStr = "";//及时清空
		}
		formula = sb.toString();
		return formula;
	}
	
	/**
	 * 计算公式中的行，导致行移动的插入才更新
	 * @param numStr   公式中的数字
	 * @param moveRowCount 移动行距
	 * @param insertPoint  从0开始，遵守poi的规则
	 * @return
	 */
	private static String calFormulaNum(String numStr,int moveRowCount,int insertPoint){
		insertPoint++;//对应到公式中的行，从1开始
		int rowNum = Integer.parseInt(numStr);
		String sRowNum = String.valueOf(rowNum); //更新前公式中的行
		String tRowNum = String.valueOf(rowNum + moveRowCount); //更新后公式中的行
		String reRow = "";//计算新的公式中的数
		if(insertPoint<=rowNum){ //上方或者本行插入，都要后移，所以需要更新公式
			reRow = tRowNum;
		}
		else{ //
			reRow = sRowNum;
		}
		return reRow;
	}
	

	/**
	 * 由于insert导致其后方的行号需要移动，此时公式的变化为，大于插入行号的需要移动，小于的不用变动
	 * @param formula    移动之前的公式
	 * @param moveRowCount  移动的行数
	 * @param insertRowNum  导致移动的行号,公式中比此数大的行才更新，如果比此行小，则不用更新
	 * @return
	 */
	public static String moveFormulaByInsert(String formula,int moveRowCount,int insertRowNum){
		return moveFormula(formula,moveRowCount,insertRowNum);
	}
	
	/**
	 * 
	 * @param formula    计算之前的公式
	 * @param moveRowCount    移动行
	 * @param moveColCount    移动列
	 * @return   计算之后的公式
	 */
	public static String moveFormula(){
		//TODO:no implement
		return null;
	}
	
	/**
	 * 在指定的位置插入给定的行
	 * @param row   需要插入的行对象，数据和样式
	 * @param rowNum 插入的行数，插入前的行后移
	 */
    public static void insertRow(HSSFSheet sheet,HSSFRow row,int rowIndex){
    	sheet.shiftRows(rowIndex, sheet.getLastRowNum(), 1);
    	HSSFRow insertRow = sheet.getRow(rowIndex);
    	if(insertRow == null)
    		insertRow = sheet.createRow(rowIndex);
    	copyRow(row, insertRow);
    }
    
    public static void insertRow(HSSFSheet sheet,HSSFRow row,int rowIndex,int rowCount){
    	
    }
    
    
    /**
     * 移动行及其后所有行
     * @param sheet   sheet
     * @param startRowIndex   移动开始行，移动行的行号
     * @param moveCount    移动的距离eg 5 向后移动5行
     */
    public static void moveRow(HSSFSheet sheet,int startRowIndex,int moveCount){
    	moveRow(sheet,startRowIndex,moveCount,startRowIndex-moveCount-1);
    }
    
    /**
     * 移动行
     * @param sheet   处理的sheet页
     * @param startRowIndex 移动开始行
     * @param moveCount  需要移动的行数,不支持负数的移动
     * @param insertPoint 引起移动的插入点，用来移动公式
     */
    public static void moveRow(HSSFSheet sheet,int startRowIndex,int moveCount,int insertPoint){
    	if(moveCount <= 0)
    		return;
    	insertPoint = (insertPoint > 0 ? insertPoint : 0);
    	int lastRowNum = sheet.getLastRowNum();
    	sheet.shiftRows(startRowIndex, lastRowNum, moveCount);
    	
    	//移动之后的范围
    	startRowIndex += moveCount;
    	lastRowNum += moveCount;
    	//更新移动之后的行的公式
    	String formula = "";
    	for(int i=startRowIndex;i<lastRowNum;i++){
    		HSSFRow row = sheet.getRow(i);
    		for(int j=0;j<row.getLastCellNum();j++){
    			HSSFCell cell = row.getCell(j);
    			if(cell!= null && cell.getCellType()== cell.CELL_TYPE_FORMULA){
    				//移动公式，引起的变化点是移动之前的行，并且需要转换为excel的自然行号，下标从1开始
    				formula = cell.getCellFormula();
    				formula = moveFormula(formula, moveCount,insertPoint + 1);
    				cell.setCellFormula(formula);
    			}
    		}
    	}
    }
    
    /**
     * 给cell设置值，针对对应的对象设置对应的值
     * @param cell
     * @param value
     */
    public static void setCellValue(HSSFCell cell,Object value){
    	if(value == null)
    		return;
    	else if(value instanceof Integer){
    		cell.setCellValue((Integer)value);
    	}
    	else if(value instanceof Long){
    		cell.setCellValue(((Long)value).intValue());
    	}
    	else if(value instanceof Date){
    		cell.setCellValue((Date)value);
    	}
    	else if(value instanceof Calendar){
    		cell.setCellValue((Calendar)value);
    	}
    	else if(value instanceof Double){
    		cell.setCellValue((Double)value);
    	}
    	else if(value instanceof BigDecimal){
    		cell.setCellValue(((BigDecimal)value).doubleValue());
    	}
    	else{
    		cell.setCellValue(value.toString());
    	}
    }
   
}
