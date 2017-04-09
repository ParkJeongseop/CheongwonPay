/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class Analyze {
	public static void main(String[] args) throws IOException {try {
           Connection con = null;
           con = DriverManager.getConnection("jdbc:mysql://localhost/cheongwonpaydb", "testuser", "testuserpassword");

           java.sql.Statement st = null;
           ResultSet rs = null;
           st = con.createStatement();
           rs = st.executeQuery("SHOW DATABASES");

           int countClub = 0;//총 동아리 수
           if (st.execute("SELECT COUNT(*) FROM club")) {//club칼럼에 있는 데이터 수를 받아온다.
			   rs = st.getResultSet();
		   }
	       while (rs.next()) {
	           countClub = rs.getInt(1);//받아온 값을 countClub에 넣어준다.
	       }
	       
           //부스 분석
           PrintWriter pw = new PrintWriter("C:\\Cheongwon Festival\\booth.csv");//booth.csv에 파일출력
           pw.println("Club_Name, Income, Visits, Type, 9시~12시, 12시~1시, 1시~2시, 2시~3시, 3시~4시, 4시~5시, 5시~6시");//파일에 출력한다.
           for(int Club_Num = 1; Club_Num<=countClub; Club_Num++)//총 동아리수가 될때까지 Club_Num을 1씩 올리며 실행한다.
           {
        	   String Club_Name = null, Type = null;//"Club_Name"은 동아리명, "Type"은 학교구분(남고는 M, 여고는 W, 연합동아리는 U)을 문자 데이터타입으로 저장한다.
        	   int Income = 0, Visits = 0;//"Income"은 수입, "Visits"는 총 방문자 수를 정수 데이터타입으로 저장한다.
        	   int Visit[] = {0, 0, 0, 0, 0, 0, 0};//"Visit"는 시간별 방문자 수를 정수 배열로 저장한다.
        	   if (st.execute("SELECT * FROM club where Club_Num='"+Club_Num+"'")) {//Club_Num인 동아리 데이터를 선택한다.
    			   rs = st.getResultSet();
    		   }
    		   
    	       while (rs.next()) {
    	           Club_Name = rs.getString("Name");//DB에서 불러온 동아리명을 "Club_Name"에 저장한다.
    	           Income = rs.getInt("Income");//DB에서 불러온 수입을 "Income"에 저장한다.
    	           Visits = rs.getInt("Visits");//DB에서 불러온 총 방문자 수를 "Visits"에 저장한다.
    	           Type = rs.getString("School");//DB에서 불러온 학교구분을 "Type"에 저장한다.
    	       }
    	       
    	       if(!Type.equals("M"))//학교구분이 남고가 아니면, 즉 여고와 연합동아리일때
    	       {
    	    	   if (st.execute("SELECT COUNT(*) FROM transactions where Club_Num='"+Club_Num+"' AND time_format(Time, '%H-%I') BETWEEN  '09-00' AND '11-59'")) {//transactions칼럼에서 9시부터 11시59분까지의 데이터의 수를 구한다.
        			   rs = st.getResultSet();
        		   }
        		   
        	       while (rs.next()) {
        	           Visit[0] = rs.getInt(1);//DB에서 불러온 데이터의 수를 "Visit[0]"에 저장한다.
        	       }
    	       }
    	       
    	       for(int i = 1 ,t=12; i<=6; i++,t++)//9시~12시를 제외한 6번의 시간분류별 수치 반복
    	       {
    	    	   if (st.execute("SELECT COUNT(*) FROM transactions where Club_Num='"+Club_Num+"' AND time_format(Time, '%H-%I') BETWEEN  '"+t+"' AND '"+t+"-59'")) {//transactions칼럼에서 t시부터 t시59분까지의 데이터의 수를 구한다.
        			   rs = st.getResultSet();
        		   }
        		   
        	       while (rs.next()) {
        	           Visit[i] = rs.getInt(1);//DB에서 불러온 데이터의 수를 "Visit[i]"에 저장한다.
        	       }
    	       }
    	       
    	       pw.println(Club_Name + "," + Income + "," + Visits + "," + Type + "," + Visit[0] + "," + Visit[1] + "," + Visit[2] + "," + Visit[3] + "," + Visit[4] + "," + Visit[5] + "," + Visit[6]);//위에서 구한 값들을 파일에 출력한다.
           }
           pw.close();//파일 출력을 끝낸다.
           
	       //출석
           PrintWriter pwat = new PrintWriter("C:\\Cheongwon Festival\\attendance.csv");//attendance.csv에 파일출력
           pwat.println("Name" + "," + "Grade" + "," + "Class" + "," + "Number" + "," + "School" + "," + "Visits");//파일에 출력한다.
           if (st.execute("SELECT * FROM user where school is not null")) {//임시학생증 발급으로 인한 trash데이터 제외
			   rs = st.getResultSet();
		   }
		   
           while (rs.next()) {
	    	   String school = rs.getString("School");//"school"은 학교구분을 문자 데이터타입으로 저장한다.
	    	   switch (school) {//남고와 여고의 출석기준의 차이로 switch문으로 각각 구분하였다.
	    	   	case "M"://남고학생일때
	    	   		if (rs.getInt("Visits")<3 && rs.getInt("Club_Num") == 0){//동아리 이용횟수가 3미만이고 부원등록이 안되어있을 때
	    	   			pwat.println(rs.getNString("Name") + "," + rs.getInt("Grade") + "," + rs.getInt("Class") + "," + rs.getInt("Number") + "," + rs.getString("School") + "," + rs.getInt("Visits"));//파일에 미출결자 명단과 데이터를 출력한다.
	    	   		}
	    	   		break;
	    	   	case "W"://여고학생일때
	    	   		if (rs.getInt("Visits")<5){//동아리 이용횟수가 5미만일때
	    	   			pwat.println(rs.getNString("Name") + "," + rs.getInt("Grade") + "," + rs.getInt("Class") + "," + rs.getInt("Number") + "," + rs.getString("School") + "," + rs.getInt("Visits"));//파일에 미출결자 명단과 데이터를 출력한다.
	    	   		}
	    	   		break;
	    	   }
	           }
	       pwat.close();//파일 출력을 끝낸다.
	       
        } catch (SQLException sqex) {//SQL오류시 오류내용 출력
           System.out.println("SQLException: " + sqex.getMessage());
           System.out.println("SQLState: " + sqex.getSQLState());
        }
	System.out.println("Analyze Finished!");
     }
}