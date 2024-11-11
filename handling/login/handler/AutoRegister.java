package handling.login.handler;

import client.LoginCryptoLegacy;
import client.MapleClient;
import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;

public class AutoRegister {
    public static final int ACCOUNTS_IP_COUNT = 9999;
    public static int fm;
    public static final boolean AutoRegister = true;
    public static boolean CheckAccount(String id) {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getConnection();
            ps = con.prepareStatement("SELECT name FROM accounts WHERE name = ?");
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.first()) {
                return true;
            }
            rs.close();
            ps.close();
            con.close();
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close(); 
                }
                if (con != null) {
                    con.close(); 
                }
            } catch (Exception e) {
                
            }
        }
        return false;
    }
    
    public static void createAccount(String id, String pwd, String ip, final MapleClient c) {
        Connection con = null;
        PreparedStatement ipc = null;
        PreparedStatement ps = null;
                
        ResultSet rs = null;
        
        int AdminIP = 0;
        try {
            con = DatabaseConnection.getConnection();
            ipc = con.prepareStatement("SELECT SessionIP FROM accounts WHERE SessionIP = ?");
            ipc.setString(1, ip);
            rs = ipc.executeQuery();
            fm = id.indexOf("m_");
            if (rs.first() == false || rs.last() == true && rs.getRow() < ACCOUNTS_IP_COUNT + AdminIP) {
                try {
                    ps = con.prepareStatement("INSERT INTO accounts (name, password, email, birthday, macs, SessionIP,gender,lastlogin) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP())");
                    ps.setString(1, id);
                    ps.setString(2, LoginCryptoLegacy.hashPassword(pwd));
                    ps.setString(3, "no@email.com");
                    ps.setString(4, "2013-12-25");
                    ps.setString(5, "00-00-00-00-00-00");
                    ps.setString(6, ip);
                    if (fm == 0) {
                        ps.setString(7, "0");
                    } else {
                        ps.setString(7, "1");
                    }
                    ps.executeUpdate();
                    c.clearInformation();
                    c.sendPacket(LoginPacket.getLoginFailed(20));
                    c.sendPacket(MaplePacketCreator.serverNotice(1, "회원가입이 완료되었습니다.\r\n\r\n아이디 : " + id + "\r\n비밀번호 : " + pwd + "\r\n\r\n성별은 여자로 시작합니다.\r\n계정앞에 m_ 붙이면 남자로 시작합니다."));
                } catch (SQLException ex) {
                    System.out.println(ex);
                }
            } else {
                c.clearInformation();
                c.sendPacket(LoginPacket.getLoginFailed(20));
                c.sendPacket(MaplePacketCreator.serverNotice(1, "회원가입 제한 횟수를 초과하였습니다.\r\n계정생성이 불가능합니다."));
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        } finally {
            try {
                if (rs != null) { rs.close(); } if (ps != null) { ps.close(); } if (ipc != null) { ipc.close(); } if (con != null) { con.close(); }
            } catch (Exception e) {
            }
        }
    }
}