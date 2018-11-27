package fingercollect;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import net.sf.json.JSONObject;

import com.zkteco.biometric.FingerprintSensorEx;
import com.zkteco.biometric.FingerprintSensorErrorCode;

public class fingerui {

	private JFrame frame;
	JButton btnImg;
	JComboBox comFingerSelect;
	private JTextField textStudentNum;
	private JTextField textStudentName;
	private JTextPane textArea;

	//the width of fingerprint image
	int fpWidth = 0;
	//the height of fingerprint image
	int fpHeight = 0;
	//for verify test
	private byte[] lastRegTemp = new byte[2048];
	//the length of lastRegTemp
	private int cbRegTemp = 0;
	//pre-register template
	private byte[][] regtemparray = new byte[3][2048];
	//Register
	private boolean bRegister = false;
	//Identify
	private boolean bIdentify = true;
	private int iFid = 1;
	private int nFakeFunOn = 1;
	//must be 3
	static final int enroll_cnt = 3;
	private int enroll_idx = 0;
	private byte[] imgbuf = null;
	private byte[] template = new byte[2048];
	private int[] templateLen = new int[1];
	private boolean mbStop = true;
	private long mhDevice = 0;
	private long mhDB = 0;
	private WorkThread workThread = null;
	private String confirmedFinger = null;
	private CloseableHttpClient httpClient;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					fingerui window = new fingerui();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public fingerui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setResizable(false);
		frame.setFont(new Font("Dialog", Font.PLAIN, 24));
		frame.setBounds(100, 100, 733, 472);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel lblTitle = new JLabel("\u6307\u7EB9\u91C7\u96C6\u7CFB\u7EDF");
		lblTitle.setBounds(0, 0, 172, 37);
		lblTitle.setFont(new Font("FangSong", Font.PLAIN, 26));
		frame.getContentPane().add(lblTitle);
		
		btnImg = new JButton();
		btnImg.setBackground(Color.CYAN);
		btnImg.setBounds(204, 37, 260, 320);
		frame.getContentPane().add(btnImg);
		
		JButton btnEnroll = new JButton("\u91C7\u96C6\u6307\u7EB9");
		btnEnroll.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
		btnEnroll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 == mhDevice)
				{
					textArea.setText("请先打开指纹采集设备！");
					return;
				}
				String Name = textStudentName.getText();
				if (Name.length() == 0) {
					textArea.setText("请先获取学生信息！！");
					return;
				}
				System.out.println("enroll: comFingerSelect index=" + comFingerSelect.getSelectedIndex());
				if (comFingerSelect.getSelectedIndex() == 0) {
					textArea.setText("请先选择要录入的手指！！");
					return;
				}
				if(!bRegister)
				{
					enroll_idx = 0;
					bRegister = true;
					textStudentNum.setEnabled(false);
					comFingerSelect.setEnabled(false);
					textArea.setText("请务必采集指纹 3 次！");
				}
			}
		});
		btnEnroll.setBounds(10, 101, 137, 40);
		frame.getContentPane().add(btnEnroll);
		
		JButton btnVerify = new JButton("\u9A8C\u8BC1\u6307\u7EB9");
		btnVerify.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
		btnVerify.setBounds(10, 152, 137, 40);
		frame.getContentPane().add(btnVerify);
		btnVerify.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 == mhDevice)
				{
					textArea.setText("请先打开指纹采集设备！");
					return;
				}
				if(bRegister)
				{
					enroll_idx = 0;
					bRegister = false;
				}
				if(!bIdentify)
				{
					bIdentify = true;
				}
				textStudentNum.setEnabled(true);
				comFingerSelect.setEnabled(true);
				textArea.setText("请在指纹采集器上按压指定的手指。");
			}
		});
		
		JButton btnClean = new JButton("\u6E05\u9664\u6307\u7EB9");
		btnClean.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
		btnClean.setBounds(10, 203, 137, 40);
		frame.getContentPane().add(btnClean);
		btnClean.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enroll_idx = 0;
				bRegister = false;
				bIdentify = false;
				btnImg.setIcon(null);
				textArea.setText("清除成功，请开始录入新的指纹！");
				textStudentNum.setEnabled(true);
				comFingerSelect.setEnabled(true);
				confirmedFinger = null;
			}
		});
		
		textStudentNum = new JTextField();
		textStudentNum.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
		textStudentNum.setBounds(494, 61, 161, 30);
		frame.getContentPane().add(textStudentNum);
		textStudentNum.setColumns(10);
		
		JLabel lblStudentNum = new JLabel("\u5B66\u751F\u7F16\u53F7\uFF1A");
		lblStudentNum.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
		lblStudentNum.setBounds(494, 37, 93, 24);
		frame.getContentPane().add(lblStudentNum);
		
		textStudentName = new JTextField();
		textStudentName.setEditable(false);
		textStudentName.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
		textStudentName.setColumns(10);
		textStudentName.setBounds(494, 111, 161, 30);
		frame.getContentPane().add(textStudentName);
		
		JLabel lblStudentName = new JLabel("\u5B66\u751F\u59D3\u540D\uFF1A");
		lblStudentName.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
		lblStudentName.setBounds(494, 90, 93, 24);
		frame.getContentPane().add(lblStudentName);
		
		JButton btnGetStudent = new JButton("\u83B7\u53D6\u5B66\u751F\u4FE1\u606F");
		btnGetStudent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getStudentInfo();
			}
		});
		btnGetStudent.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
		btnGetStudent.setBounds(494, 160, 161, 44);
		frame.getContentPane().add(btnGetStudent);
		
		JButton btnOpen = new JButton("\u6253\u5F00\u6307\u7EB9\u8BBE\u5907");
		btnOpen.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
		btnOpen.setBounds(10, 287, 137, 23);
		frame.getContentPane().add(btnOpen);
		btnOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openDevice(e);
			}
		});
		
		JButton btnClose = new JButton("\u5173\u95ED\u6307\u7EB9\u8BBE\u5907");
		btnClose.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
		btnClose.setBounds(10, 321, 137, 23);
		frame.getContentPane().add(btnClose);
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				FreeSensor();
				textArea.setText("已关闭指纹采集器！");
			}
		});
		
		JButton btnUpdate = new JButton("\u63D0\u4EA4\u6307\u7EB9\u4FE1\u606F");
		btnUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateStudentFingerInfo();
			}
		});
		btnUpdate.setBackground(Color.ORANGE);
		btnUpdate.setForeground(Color.BLACK);
		btnUpdate.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
		btnUpdate.setBounds(494, 320, 161, 116);
		frame.getContentPane().add(btnUpdate);
		
		textArea = new JTextPane();
		textArea.setForeground(Color.BLACK);
		textArea.setEditable(false);
		textArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
		textArea.setBounds(10, 381, 454, 55);
		frame.getContentPane().add(textArea);
		
		comFingerSelect = new JComboBox();
		comFingerSelect.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
		comFingerSelect.setModel(new DefaultComboBoxModel(new String[] {"请选择手指", "0.左小指姆", "1.左无名指", "2.左中指姆", "3.左食指姆", "4.左大指姆", "5.右大指姆", "6.右食指姆", "7.右中指姆", "8.右无名指", "9.右小指姆"}));
		comFingerSelect.setBounds(10, 51, 137, 30);
		frame.getContentPane().add(comFingerSelect);
		comFingerSelect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] fingerstr = ((String) comFingerSelect.getSelectedItem()).split("\\.");
				textArea.setText("选择指姆：" + fingerstr[1] + ",id为" + fingerstr[0]);
				System.out.println(fingerstr[0]+"."+fingerstr[1]);
			}
		});
		
		JLabel lblInfo = new JLabel("信息提示：");
		lblInfo.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
		lblInfo.setBounds(10, 355, 93, 24);
		frame.getContentPane().add(lblInfo);
		btnImg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		
		httpClient = HttpClients.createDefault();
	}
	private void openDevice (ActionEvent e) {
		if (0 != mhDevice)
		{
			//already inited
			textArea.setText("Please close device first!");
			return;
		}
		int ret = FingerprintSensorErrorCode.ZKFP_ERR_OK;
		//Initialize
		cbRegTemp = 0;
		bRegister = false;
		bIdentify = false;
		iFid = 1;
		enroll_idx = 0;
		if (FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init())
		{
			textArea.setText("初始化指纹采集器失败!!!");
			return;
		}
		ret = FingerprintSensorEx.GetDeviceCount();
		if (ret < 0)
		{
			textArea.setText("连接指纹采集器失败!!!");
			FreeSensor();
			return;
		}
		if (0 == (mhDevice = FingerprintSensorEx.OpenDevice(0)))
		{
			textArea.setText("打开采集器失败（" + ret + "）!");
			FreeSensor();
			return;
		}
		if (0 == (mhDB = FingerprintSensorEx.DBInit()))
		{
			textArea.setText("初始化指纹数据库失败（" + ret + "）!");
			FreeSensor();
			return;
		}
		
		//For ISO/Ansi
		int nFmt = 0; //0: Ansi, 1: ISO

		FingerprintSensorEx.DBSetParameter(mhDB,  5010, nFmt);				
		//For ISO/Ansi End
		
		//set fakefun off
		//FingerprintSensorEx.SetParameter(mhDevice, 2002, changeByte(nFakeFunOn), 4);
		
		byte[] paramValue = new byte[4];
		int[] size = new int[1];
		//GetFakeOn
		//size[0] = 4;
		//FingerprintSensorEx.GetParameters(mhDevice, 2002, paramValue, size);
		//nFakeFunOn = byteArrayToInt(paramValue);
		
		size[0] = 4;
		FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size);
		fpWidth = byteArrayToInt(paramValue);
		size[0] = 4;
		FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size);
		fpHeight = byteArrayToInt(paramValue);
		//width = fingerprintSensor.getImageWidth();
		//height = fingerprintSensor.getImageHeight();
		imgbuf = new byte[fpWidth*fpHeight];
		System.out.println("opendevice img resize:"+fpWidth+","+fpHeight);
		// btnImg.resize(fpWidth, fpHeight);
		mbStop = false;
		workThread = new WorkThread();
	    workThread.start();
        textArea.setText("指纹采集设备打开成功！输入学生学号，点击获取学生信息，以确认学生信息已登记过。");
	}
	
	public static void writeBitmap(byte[] imageBuf, int nWidth, int nHeight,
			String path) throws IOException {
		java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
		java.io.DataOutputStream dos = new java.io.DataOutputStream(fos);

		int w = (((nWidth+3)/4)*4);
		int bfType = 0x424d; // 浣嶅浘鏂囦欢绫诲瀷锛�0鈥�1瀛楄妭锛�
		int bfSize = 54 + 1024 + w * nHeight;// bmp鏂囦欢鐨勫ぇ灏忥紙2鈥�5瀛楄妭锛�
		int bfReserved1 = 0;// 浣嶅浘鏂囦欢淇濈暀瀛楋紝蹇呴』涓�0锛�6-7瀛楄妭锛�
		int bfReserved2 = 0;// 浣嶅浘鏂囦欢淇濈暀瀛楋紝蹇呴』涓�0锛�8-9瀛楄妭锛�
		int bfOffBits = 54 + 1024;// 鏂囦欢澶村紑濮嬪埌浣嶅浘瀹為檯鏁版嵁涔嬮棿鐨勫瓧鑺傜殑鍋忕Щ閲忥紙10-13瀛楄妭锛�

		dos.writeShort(bfType); // 杈撳叆浣嶅浘鏂囦欢绫诲瀷'BM'
		dos.write(changeByte(bfSize), 0, 4); // 杈撳叆浣嶅浘鏂囦欢澶у皬
		dos.write(changeByte(bfReserved1), 0, 2);// 杈撳叆浣嶅浘鏂囦欢淇濈暀瀛�
		dos.write(changeByte(bfReserved2), 0, 2);// 杈撳叆浣嶅浘鏂囦欢淇濈暀瀛�
		dos.write(changeByte(bfOffBits), 0, 4);// 杈撳叆浣嶅浘鏂囦欢鍋忕Щ閲�

		int biSize = 40;// 淇℃伅澶存墍闇�鐨勫瓧鑺傛暟锛�14-17瀛楄妭锛�
		int biWidth = nWidth;// 浣嶅浘鐨勫锛�18-21瀛楄妭锛�
		int biHeight = nHeight;// 浣嶅浘鐨勯珮锛�22-25瀛楄妭锛�
		int biPlanes = 1; // 鐩爣璁惧鐨勭骇鍒紝蹇呴』鏄�1锛�26-27瀛楄妭锛�
		int biBitcount = 8;// 姣忎釜鍍忕礌鎵�闇�鐨勪綅鏁帮紙28-29瀛楄妭锛夛紝蹇呴』鏄�1浣嶏紙鍙岃壊锛夈��4浣嶏紙16鑹诧級銆�8浣嶏紙256鑹诧級鎴栬��24浣嶏紙鐪熷僵鑹诧級涔嬩竴銆�
		int biCompression = 0;// 浣嶅浘鍘嬬缉绫诲瀷锛屽繀椤绘槸0锛堜笉鍘嬬缉锛夛紙30-33瀛楄妭锛夈��1锛圔I_RLEB鍘嬬缉绫诲瀷锛夋垨2锛圔I_RLE4鍘嬬缉绫诲瀷锛変箣涓�銆�
		int biSizeImage = w * nHeight;// 瀹為檯浣嶅浘鍥惧儚鐨勫ぇ灏忥紝鍗虫暣涓疄闄呯粯鍒剁殑鍥惧儚澶у皬锛�34-37瀛楄妭锛�
		int biXPelsPerMeter = 0;// 浣嶅浘姘村钩鍒嗚鲸鐜囷紝姣忕背鍍忕礌鏁帮紙38-41瀛楄妭锛夎繖涓暟鏄郴缁熼粯璁ゅ��
		int biYPelsPerMeter = 0;// 浣嶅浘鍨傜洿鍒嗚鲸鐜囷紝姣忕背鍍忕礌鏁帮紙42-45瀛楄妭锛夎繖涓暟鏄郴缁熼粯璁ゅ��
		int biClrUsed = 0;// 浣嶅浘瀹為檯浣跨敤鐨勯鑹茶〃涓殑棰滆壊鏁帮紙46-49瀛楄妭锛夛紝濡傛灉涓�0鐨勮瘽锛岃鏄庡叏閮ㄤ娇鐢ㄤ簡
		int biClrImportant = 0;// 浣嶅浘鏄剧ず杩囩▼涓噸瑕佺殑棰滆壊鏁�(50-53瀛楄妭)锛屽鏋滀负0鐨勮瘽锛岃鏄庡叏閮ㄩ噸瑕�

		dos.write(changeByte(biSize), 0, 4);// 杈撳叆淇℃伅澶存暟鎹殑鎬诲瓧鑺傛暟
		dos.write(changeByte(biWidth), 0, 4);// 杈撳叆浣嶅浘鐨勫
		dos.write(changeByte(biHeight), 0, 4);// 杈撳叆浣嶅浘鐨勯珮
		dos.write(changeByte(biPlanes), 0, 2);// 杈撳叆浣嶅浘鐨勭洰鏍囪澶囩骇鍒�
		dos.write(changeByte(biBitcount), 0, 2);// 杈撳叆姣忎釜鍍忕礌鍗犳嵁鐨勫瓧鑺傛暟
		dos.write(changeByte(biCompression), 0, 4);// 杈撳叆浣嶅浘鐨勫帇缂╃被鍨�
		dos.write(changeByte(biSizeImage), 0, 4);// 杈撳叆浣嶅浘鐨勫疄闄呭ぇ灏�
		dos.write(changeByte(biXPelsPerMeter), 0, 4);// 杈撳叆浣嶅浘鐨勬按骞冲垎杈ㄧ巼
		dos.write(changeByte(biYPelsPerMeter), 0, 4);// 杈撳叆浣嶅浘鐨勫瀭鐩村垎杈ㄧ巼
		dos.write(changeByte(biClrUsed), 0, 4);// 杈撳叆浣嶅浘浣跨敤鐨勬�婚鑹叉暟
		dos.write(changeByte(biClrImportant), 0, 4);// 杈撳叆浣嶅浘浣跨敤杩囩▼涓噸瑕佺殑棰滆壊鏁�

		for (int i = 0; i < 256; i++) {
			dos.writeByte(i);
			dos.writeByte(i);
			dos.writeByte(i);
			dos.writeByte(0);
		}

		byte[] filter = null;
		if (w > nWidth)
		{
			filter = new byte[w-nWidth];
		}
		
		for(int i=0;i<nHeight;i++)
		{
			dos.write(imageBuf, (nHeight-1-i)*nWidth, nWidth);
			if (w > nWidth)
				dos.write(filter, 0, w-nWidth);
		}
		dos.flush();
		dos.close();
		fos.close();
	}
	public static byte[] changeByte(int data) {
		return intToByteArray(data);
	}
	
	public static byte[] intToByteArray (final int number) {
		byte[] abyte = new byte[4];  
	    abyte[0] = (byte) (0xff & number);  
	    abyte[1] = (byte) ((0xff00 & number) >> 8);  
	    abyte[2] = (byte) ((0xff0000 & number) >> 16);  
	    abyte[3] = (byte) ((0xff000000 & number) >> 24);  
	    return abyte; 
	}	 
		 
	public static int byteArrayToInt(byte[] bytes) {
		int number = bytes[0] & 0xFF;  
	    number |= ((bytes[1] << 8) & 0xFF00);  
	    number |= ((bytes[2] << 16) & 0xFF0000);  
	    number |= ((bytes[3] << 24) & 0xFF000000);  
	    return number;  
	}
	
	private class WorkThread extends Thread {
        @Override
        public void run() {
            super.run();
            int ret = 0;
            while (!mbStop) {
            	templateLen[0] = 2048;
            	if (0 == (ret = FingerprintSensorEx.AcquireFingerprint(mhDevice, imgbuf, template, templateLen)))
            	{
            		if (nFakeFunOn == 1)
                	{
                		byte[] paramValue = new byte[4];
        				int[] size = new int[1];
        				size[0] = 4;
        				int nFakeStatus = 0;
        				//GetFakeStatus
        				ret = FingerprintSensorEx.GetParameters(mhDevice, 2004, paramValue, size);
        				nFakeStatus = byteArrayToInt(paramValue);
        				System.out.println("ret = "+ ret +",nFakeStatus=" + nFakeStatus);
        				if (0 == ret && (byte)(nFakeStatus & 31) != 31)
        				{
        					textArea.setText("请用正确的手指进行采集录入！");
        					return;
        				}
                	}
                	OnCatpureOK(imgbuf);
                	OnExtractOK(template, templateLen[0]);
            	}
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

		private void runOnUiThread(Runnable runnable) {
			// TODO Auto-generated method stub
			
		}
    }
	private void OnCatpureOK(byte[] imgBuf)
	{
		try {
			writeBitmap(imgBuf, fpWidth, fpHeight, "fingerprint.bmp");
			btnImg.setIcon(new ImageIcon(ImageIO.read(new File("fingerprint.bmp"))));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void OnExtractOK(byte[] template, int len)
	{
		if(bRegister)
		{
			int[] fid = new int[1];
			int[] score = new int [1];
            int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
            System.out.println("OnExtractOK:" + len);

            if (ret == 0)
            {
                textArea.setText("这个指姆已经采集了 ,id=" + fid[0] + ",无需再次采集");
                bRegister = false;
                enroll_idx = 0;
                return;
            }
            if (enroll_idx > 0 && FingerprintSensorEx.DBMatch(mhDB, regtemparray[enroll_idx-1], template) <= 0)
            {
            	textArea.setText("在指纹采集的时候，请用相同的指姆按压3次来完成指纹录入");
                return;
            }
            System.arraycopy(template, 0, regtemparray[enroll_idx], 0, 2048);
            enroll_idx++;
            if (enroll_idx == 3) {
            	int[] _retLen = new int[1];
                _retLen[0] = 2048;
                byte[] regTemp = new byte[_retLen[0]];
                if (0 == (ret = FingerprintSensorEx.DBMerge(mhDB, regtemparray[0], regtemparray[1], regtemparray[2], regTemp, _retLen)) &&
                		0 == (ret = FingerprintSensorEx.DBAdd(mhDB, iFid, regTemp))) {
                	iFid++;
                	cbRegTemp = _retLen[0];
                    System.arraycopy(regTemp, 0, lastRegTemp, 0, cbRegTemp);
                    
                    //Base64 Template
                    System.out.println( _retLen[0]);
                    
                    confirmedFinger = FingerprintSensorEx.BlobToBase64(lastRegTemp,_retLen[0]);
                    System.out.println(confirmedFinger);
                    System.out.println("enroll succ");
                    textArea.setText("指纹采集完成！ 请验证指纹或者直接提交指纹信息。");
                } else {
                	textArea.setText("指纹采集失败, error code=" + ret + ",请重新采集！！");
                }
                bRegister = false;
            } else {
            	textArea.setText("您已经采集指纹  " + (3 - enroll_idx) + " 次 ");
            }
		}
		else
		{
			if (bIdentify)
			{
				int[] fid = new int[1];
				int[] score = new int [1];
				int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
                if (ret == 0)
                {
                	textArea.setText("指纹认证成功！, fid=" + fid[0] + ",score=" + score[0]);
                }
                else
                {
                	textArea.setText("指纹认证失败, errcode=" + ret);
                }
                    
			}
			else
			{
				if(cbRegTemp <= 0)
				{
					textArea.setText("请先采集指纹！！！");
				}
				else
				{
					int ret = FingerprintSensorEx.DBMatch(mhDB, lastRegTemp, template);
					if(ret > 0)
					{
						textArea.setText("验证成功, 匹配分数=" + ret);
					}
					else
					{
						textArea.setText("验证失败, ret=" + ret);
					}
				}
			}
		}
	}
	private void FreeSensor()
	{
		mbStop = true;
		try {		//wait for thread stopping
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (0 != mhDB)
		{
			FingerprintSensorEx.DBFree(mhDB);
			mhDB = 0;
		}
		if (0 != mhDevice)
		{
			FingerprintSensorEx.CloseDevice(mhDevice);
			mhDevice = 0;
		}
		FingerprintSensorEx.Terminate();
	}
	private void getStudentInfo () {
		String stuNo = textStudentNum.getText();
		if (stuNo.length() == 0) {
			textArea.setText("请先输入学生编号！再点击获取学生信息。");
			return;
		}
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("num", stuNo));
		String content = null;
		try {
			String str = EntityUtils.toString(new UrlEncodedFormEntity(nameValuePairs,Consts.UTF_8));
			HttpGet httpget=new HttpGet("http://172.16.119.130:8010/public/index.php/index/index/getmethod?"+str);
			System.out.println("http://172.16.119.130:8010/public/index.php/index/index/getmethod?"+str);
			CloseableHttpResponse response = httpClient.execute(httpget);
			HttpEntity entity = response.getEntity();
			content = EntityUtils.toString(entity, "utf-8");
		}catch (Exception ex) {
			textArea.setText("获取学生信息失败，请检测网络是否畅通。");
			return;
		}
		String inputStudentNo = textStudentNum.getText();
		if (content == null) {
			textArea.setText("微校云服务没有查找到学号 " + inputStudentNo + "的学生。请查看学号是否输入正确。");
			return;
		} else {
			System.out.println(content);
			JSONObject jsonObject = JSONObject.fromObject(content);
			String studentNo = jsonObject.getString("studentNo");
			if (studentNo.length() == 0) {
				textArea.setText("微校云服务没有查找到学号 " + inputStudentNo + "的学生。请查看学号是否输入正确。");
			} else {
				textStudentNum.setText(studentNo);
				textStudentName.setText(jsonObject.getString("studentName"));
			}
		}
	}
	private void updateStudentFingerInfo () {
		if (confirmedFinger == null) {
			textArea.setText("当前还没生成指纹模板，请先采集指纹信息后再点击提交指纹信息！");
			return;
		}
		HttpPost httpPost = new HttpPost("http://172.16.119.130:8010/public/index.php/index/index/postmethod");
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		String stuNo = textStudentNum.getText();
		String[] fingerstr = ((String) comFingerSelect.getSelectedItem()).split("\\.");
		nameValuePairs.add(new BasicNameValuePair("studentNo", stuNo));
		nameValuePairs.add(new BasicNameValuePair("fingerId", fingerstr[0]));
		nameValuePairs.add(new BasicNameValuePair("fingerName", fingerstr[1]));
		nameValuePairs.add(new BasicNameValuePair("fingerModel", confirmedFinger));
		String content = null;
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
			CloseableHttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			content = EntityUtils.toString(entity, "utf-8");
		}catch (Exception ex) {
			textArea.setText("获取学生信息失败(http post failed),请检测网络是否畅通。");
			return;
		}
		if (content == null) {
			textArea.setText("微校云服务数据验证失败！请重新提交！");
			return;
		} else {
			System.out.println(content);
			JSONObject jsonObject = JSONObject.fromObject(content);
			int status = jsonObject.getInt("status");
			if (status == 0) {
				textArea.setText("微校云服务数据校验失败！");
			} else {
				textArea.setText(textStudentName.getText() + "(" + textStudentNum.getText() + ") 的指纹信息上传成功！");
				textStudentNum.setText("");
				textStudentName.setText("");
				confirmedFinger = null;
			}
		}
	}
}
