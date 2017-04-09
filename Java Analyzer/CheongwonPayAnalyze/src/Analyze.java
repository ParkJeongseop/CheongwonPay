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

           int countClub = 0;//�� ���Ƹ� ��
           if (st.execute("SELECT COUNT(*) FROM club")) {//clubĮ���� �ִ� ������ ���� �޾ƿ´�.
			   rs = st.getResultSet();
		   }
	       while (rs.next()) {
	           countClub = rs.getInt(1);//�޾ƿ� ���� countClub�� �־��ش�.
	       }
	       
           //�ν� �м�
           PrintWriter pw = new PrintWriter("C:\\Cheongwon Festival\\booth.csv");//booth.csv�� �������
           pw.println("Club_Name, Income, Visits, Type, 9��~12��, 12��~1��, 1��~2��, 2��~3��, 3��~4��, 4��~5��, 5��~6��");//���Ͽ� ����Ѵ�.
           for(int Club_Num = 1; Club_Num<=countClub; Club_Num++)//�� ���Ƹ����� �ɶ����� Club_Num�� 1�� �ø��� �����Ѵ�.
           {
        	   String Club_Name = null, Type = null;//"Club_Name"�� ���Ƹ���, "Type"�� �б�����(����� M, ����� W, ���յ��Ƹ��� U)�� ���� ������Ÿ������ �����Ѵ�.
        	   int Income = 0, Visits = 0;//"Income"�� ����, "Visits"�� �� �湮�� ���� ���� ������Ÿ������ �����Ѵ�.
        	   int Visit[] = {0, 0, 0, 0, 0, 0, 0};//"Visit"�� �ð��� �湮�� ���� ���� �迭�� �����Ѵ�.
        	   if (st.execute("SELECT * FROM club where Club_Num='"+Club_Num+"'")) {//Club_Num�� ���Ƹ� �����͸� �����Ѵ�.
    			   rs = st.getResultSet();
    		   }
    		   
    	       while (rs.next()) {
    	           Club_Name = rs.getString("Name");//DB���� �ҷ��� ���Ƹ����� "Club_Name"�� �����Ѵ�.
    	           Income = rs.getInt("Income");//DB���� �ҷ��� ������ "Income"�� �����Ѵ�.
    	           Visits = rs.getInt("Visits");//DB���� �ҷ��� �� �湮�� ���� "Visits"�� �����Ѵ�.
    	           Type = rs.getString("School");//DB���� �ҷ��� �б������� "Type"�� �����Ѵ�.
    	       }
    	       
    	       if(!Type.equals("M"))//�б������� ���� �ƴϸ�, �� ����� ���յ��Ƹ��϶�
    	       {
    	    	   if (st.execute("SELECT COUNT(*) FROM transactions where Club_Num='"+Club_Num+"' AND time_format(Time, '%H-%I') BETWEEN  '09-00' AND '11-59'")) {//transactionsĮ������ 9�ú��� 11��59�б����� �������� ���� ���Ѵ�.
        			   rs = st.getResultSet();
        		   }
        		   
        	       while (rs.next()) {
        	           Visit[0] = rs.getInt(1);//DB���� �ҷ��� �������� ���� "Visit[0]"�� �����Ѵ�.
        	       }
    	       }
    	       
    	       for(int i = 1 ,t=12; i<=6; i++,t++)//9��~12�ø� ������ 6���� �ð��з��� ��ġ �ݺ�
    	       {
    	    	   if (st.execute("SELECT COUNT(*) FROM transactions where Club_Num='"+Club_Num+"' AND time_format(Time, '%H-%I') BETWEEN  '"+t+"' AND '"+t+"-59'")) {//transactionsĮ������ t�ú��� t��59�б����� �������� ���� ���Ѵ�.
        			   rs = st.getResultSet();
        		   }
        		   
        	       while (rs.next()) {
        	           Visit[i] = rs.getInt(1);//DB���� �ҷ��� �������� ���� "Visit[i]"�� �����Ѵ�.
        	       }
    	       }
    	       
    	       pw.println(Club_Name + "," + Income + "," + Visits + "," + Type + "," + Visit[0] + "," + Visit[1] + "," + Visit[2] + "," + Visit[3] + "," + Visit[4] + "," + Visit[5] + "," + Visit[6]);//������ ���� ������ ���Ͽ� ����Ѵ�.
           }
           pw.close();//���� ����� ������.
           
	       //�⼮
           PrintWriter pwat = new PrintWriter("C:\\Cheongwon Festival\\attendance.csv");//attendance.csv�� �������
           pwat.println("Name" + "," + "Grade" + "," + "Class" + "," + "Number" + "," + "School" + "," + "Visits");//���Ͽ� ����Ѵ�.
           if (st.execute("SELECT * FROM user where school is not null")) {//�ӽ��л��� �߱����� ���� trash������ ����
			   rs = st.getResultSet();
		   }
		   
           while (rs.next()) {
	    	   String school = rs.getString("School");//"school"�� �б������� ���� ������Ÿ������ �����Ѵ�.
	    	   switch (school) {//����� ������ �⼮������ ���̷� switch������ ���� �����Ͽ���.
	    	   	case "M"://�����л��϶�
	    	   		if (rs.getInt("Visits")<3 && rs.getInt("Club_Num") == 0){//���Ƹ� �̿�Ƚ���� 3�̸��̰� �ο������ �ȵǾ����� ��
	    	   			pwat.println(rs.getNString("Name") + "," + rs.getInt("Grade") + "," + rs.getInt("Class") + "," + rs.getInt("Number") + "," + rs.getString("School") + "," + rs.getInt("Visits"));//���Ͽ� ������� ��ܰ� �����͸� ����Ѵ�.
	    	   		}
	    	   		break;
	    	   	case "W"://�����л��϶�
	    	   		if (rs.getInt("Visits")<5){//���Ƹ� �̿�Ƚ���� 5�̸��϶�
	    	   			pwat.println(rs.getNString("Name") + "," + rs.getInt("Grade") + "," + rs.getInt("Class") + "," + rs.getInt("Number") + "," + rs.getString("School") + "," + rs.getInt("Visits"));//���Ͽ� ������� ��ܰ� �����͸� ����Ѵ�.
	    	   		}
	    	   		break;
	    	   }
	           }
	       pwat.close();//���� ����� ������.
	       
        } catch (SQLException sqex) {//SQL������ �������� ���
           System.out.println("SQLException: " + sqex.getMessage());
           System.out.println("SQLState: " + sqex.getSQLState());
        }
	System.out.println("Analyze Finished!");
     }
}