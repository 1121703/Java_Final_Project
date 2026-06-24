import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;

public class final_game extends JFrame {
    // =====================================================================
    // ★ 統一的遊戲配置數據源 - 所有資料都在此定義，避免重複
    // =====================================================================
    // 角色配置：{名稱, 檔案名, 特殊禮物名(原始角色用，淨化夥伴為null)}
    private static final String[][] CHARACTER_CONFIG = {
        // 原始 9 個角色
        {"小石頭", "stone.png", "晶瑩打磨砂"},
        {"貓頭鷹", "owl.png", "星空古銅錶"},
        {"花鹿", "deer.png", "晨露繁花環"},
        {"雲朵羊", "sheep.png", "彩虹棉花糖"},
        {"火焰狐", "fox.png", "不滅炎心石"},
        {"魔法貓", "cat.png", "懸浮魔力球"},
        {"機械狗", "robot_dog.png", "合金機關骨"},
        {"光明精靈", "elf.png", "聖潔光稜鏡"},
        {"星辰龍", "dragon.png", "璀璨星雲瓶"},
        // 淨化夥伴 5 個（對應 5 隻怪獸）
        {"守護猿", "守護猿.png", null},
        {"蔚藍水母", "蔚藍水母.png", null},
        {"吟遊羽鳥", "吟遊羽鳥.png", null},
        {"寶石貓", "寶石貓.png", null},
        {"時光精靈", "時光精靈.png", null}
    };
    
    // 怪獸配置：{名稱, 圖片檔名, HP, ATK, 故事, 區域名, 淨化為(夥伴名)}
    private static final String[][] MONSTER_CONFIG = {
        {"影之巨猿", "影之巨猿 (憤怒).png", "1500", "80", ".\\story\\s1.txt", "枯萎森林", "守護猿"},
        {"鏽蝕海怪", "鏽蝕海怪 (哀傷).png", "1800", "100", ".\\story\\s2.txt", "深海廢墟", "蔚藍水母"},
        {"無聲夜裊", "無聲夜梟 (孤獨).png", "2000", "140", ".\\story\\s3.txt", "禁錮鳥籠", "吟遊羽鳥"},
        {"貪婪晶怪", "貪婪晶怪 (慾望).png", "2300", "180", ".\\story\\s4.txt", "貪婪礦坑", "寶石貓"},
        {"時鐘亡魂", "時鐘亡魂 (焦慮).png", "2500", "200", ".\\story\\s5.txt", "崩壞時鐘", "時光精靈"}
    };

    // 獲取角色對應的等級外框顏色
    public static Color getRarityColor(String name) {
        switch (name) {
            case "小石頭": case "貓頭鷹": case "花鹿":
                return new Color(150, 150, 150); // N - 灰
            case "雲朵羊": case "火焰狐":
                return new Color(50, 205, 50);   // R - 綠
            case "魔法貓":
                return new Color(30, 144, 255);  // SR - 藍
            case "機械狗":
                return new Color(138, 43, 226);  // SSR - 紫
            case "光明精靈":
                return new Color(255, 215, 0);   // SSSR - 金
            case "星辰龍":
                return new Color(220, 20, 60);   // UR - 紅
            default:
                return Color.pink; // S（淨化夥伴）- 粉紅
        }
    }

    // 獲取角色對應的初始數值：回傳格式為 {ATK, HP}
    public static int[] getBaseStats(String name) {
        switch (name) {
            case "小石頭": case "貓頭鷹": case "花鹿":
                return new int[]{50, 100};      // N
            case "雲朵羊": case "火焰狐":
                return new int[]{60, 120};      // R
            case "魔法貓":
                return new int[]{80, 150};      // SR
            case "機械狗":
                return new int[]{120, 180};     // SSR
            case "光明精靈":
                return new int[]{150, 200};     // SSSR
            case "星辰龍":
                return new int[]{200, 250};     // UR
            default:
                // 淨化怪獸後獲得的隱藏夥伴，給予預設強悍數值 // S
                return new int[]{250, 300}; 
        }
    }
    
    // 快速查詢 Map（在靜態塊中初始化）
    private static final java.util.Map<String, String> CHARACTER_FILES = new java.util.HashMap<>();
    private static final java.util.Map<String, String> SPECIAL_GIFTS = new java.util.HashMap<>();
    
    // 靜態初始化塊，從 CHARACTER_CONFIG 中構建快速查詢的 Map
    static {
        // 從 CHARACTER_CONFIG 構建 Map
        for (String[] charData : CHARACTER_CONFIG) {
            String name = charData[0];
            String filename = charData[1];
            String gift = charData[2];
            
            CHARACTER_FILES.put(name, filename);
            if (gift != null) {  // 只有原始角色有特殊禮物
                SPECIAL_GIFTS.put(name, gift);
            }
        }
    }
    
    // 獲取禮物名稱
    public String getSpecialGiftName(String name) {
        return SPECIAL_GIFTS.getOrDefault(name, "彩色禮物盒");
    }

    // 獲取禮物圖片檔案名
    public String getSpecialGiftImg(String name) {
        String giftName = getSpecialGiftName(name);
        return giftName + ".png";
    }

    // 獲取角色檔案名稱
    public String getCharacterFileName(String name) {
        return CHARACTER_FILES.getOrDefault(name, "");
    }
    
    // 根據名稱判斷是否是有效的夥伴角色（不是怪獸）
    public static boolean isValidPartner(String name) {
        for (String[] charData : CHARACTER_CONFIG) {
            if (charData[0].equals(name)) {
                return true;  // 在CHARACTER_CONFIG中找到，是有效夥伴
            }
        }
        return false;
    }
    
    // 根據怪獸名稱查找淨化夥伴名稱
    public static String getPurifiedPartnerName(String monsterName) {
        for (String[] monster : MONSTER_CONFIG) {
            if (monster[0].equals(monsterName)) {
                return monster[6]; // 淨化為(夥伴名)
            }
        }
        return null;
    }

    // 根據區域名稱查找對應的怪獸資訊，回傳格式為 {怪獸名, 圖片路徑, HP, ATK, 故事}
    class MapWindow extends JDialog {
        // AREA_MONSTERS 動態生成，從 MONSTER_CONFIG 讀取
        // 格式：{區域名, 怪獸名, 圖片路徑, HP, ATK, 故事}
        private static String[][] generateAreaMonsters() {
            String[][] areaMonsters = new String[MONSTER_CONFIG.length][];
            for (int i = 0; i < MONSTER_CONFIG.length; i++) {
                String[] monsterData = MONSTER_CONFIG[i];
                areaMonsters[i] = new String[]{
                    monsterData[5],                               // 區域名
                    monsterData[0],                               // 怪獸名
                    ".\\Monster_image\\" + monsterData[1],        // 圖片路徑 (戰鬥時還是要傳怪獸的圖片)
                    monsterData[2],                               // HP
                    monsterData[3],                               // ATK
                    monsterData[4]                                // 故事
                };
            }
            return areaMonsters;
        }
        
        private static final String[][] AREA_MONSTERS = generateAreaMonsters();

        public MapWindow(Frame parent) {
            super(parent, "世界地圖", true);
            setLayout(new BorderLayout());
            setSize(950, 340); // 稍微加高一點點，讓排版更舒適
            setLocationRelativeTo(parent);

            JLabel title = new JLabel("選擇地區", SwingConstants.CENTER);
            title.setFont(new Font("標楷體", Font.BOLD, 26));
            title.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));
            add(title, BorderLayout.NORTH);

            JPanel areaPanel = new JPanel(new GridLayout(1, AREA_MONSTERS.length, 10, 0));
            areaPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

            for (int i = 0; i < AREA_MONSTERS.length; i++) {
                String[] info    = AREA_MONSTERS[i];
                String areaName  = info[0], monsterName = info[1], monsterImg = info[2];
                int monsterHp    = Integer.parseInt(info[3]);
                int monsterAtk   = Integer.parseInt(info[4]);
                String story     = info[5];
                
                // 第一關永遠開放；之後每關需要前一關的怪獸已被打倒才解鎖
                boolean unlocked = (i == 0) || ownedCharacters.contains(AREA_MONSTERS[i - 1][1]);

                JPanel card = new JPanel(new BorderLayout(5, 5));
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(unlocked ? new Color(60,160,60) : Color.GRAY, 2),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)));
                card.setBackground(unlocked ? new Color(225, 255, 225) : new Color(210, 210, 210));

                JLabel lblArea = new JLabel(areaName, SwingConstants.CENTER);
                lblArea.setFont(new Font("標楷體", Font.BOLD, 17));

                // ★ 修改點：將這裡載入的圖片改為 Back 資料夾下的場景圖
                JLabel imgLbl = new JLabel("", SwingConstants.CENTER);
                String bgImgPath = ".\\Back\\" + areaName + ".png"; // 對應區域背景圖路徑
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.File(bgImgPath));
                    // 因為是場景圖，改為 130x85 的橫向比例更好看
                    imgLbl.setIcon(new ImageIcon(img.getScaledInstance(130, 85, Image.SCALE_SMOOTH)));
                } catch (Exception ex) {
                    imgLbl.setText("🌲"); // 找不到圖片時的備用圖示
                    imgLbl.setFont(new Font("Serif", Font.PLAIN, 40));
                }

                JLabel lblStats = new JLabel(String.format(
                    "<html><center><b>%s</b><br>HP %d  ATK %d</center></html>",
                    monsterName, monsterHp, monsterAtk), SwingConstants.CENTER);
                lblStats.setFont(new Font("標楷體", Font.PLAIN, 13));

                JPanel mid = new JPanel(new BorderLayout(3, 8)); // 稍微增加圖片與文字間距
                mid.setOpaque(false);
                mid.add(imgLbl,   BorderLayout.CENTER);
                mid.add(lblStats, BorderLayout.SOUTH);

                JButton btnEnter = new JButton(unlocked ? "進入" : "未開放");
                btnEnter.setEnabled(unlocked);
                btnEnter.setFont(new Font("標楷體", Font.BOLD, 16));
                if (unlocked) {
                    btnEnter.setBackground(new Color(60, 160, 60));
                    btnEnter.setForeground(Color.WHITE);
                }

                final String mn = monsterName, mi = monsterImg, st = story;
                final int mhp = monsterHp, matk = monsterAtk;
                btnEnter.addActionListener(e -> {
                    dispose();
                    // ★ 修改點：先打開選擇出戰夥伴的視窗，再由夥伴選擇視窗打開故事說明
                    new PartnerSelectWindow(parent, mn, mi, mhp, matk, st);
                });

                card.add(lblArea,  BorderLayout.NORTH);
                card.add(mid,      BorderLayout.CENTER);
                card.add(btnEnter, BorderLayout.SOUTH);
                areaPanel.add(card);
            }
            add(areaPanel, BorderLayout.CENTER);
            setVisible(true);
        }
    }

    // 在 final_game 類別內新增此初始化方法
    private void initGiftInventory() {
        // 通用禮物
        String[] generalGifts = {"小禮物", "大禮物"};
        for (String gift : generalGifts) {
            if (!giftInventory.containsKey(gift)) {
                giftInventory.put(gift, 0);
            }
        }
        
        // 特殊禮物（從 SPECIAL_GIFTS 動態讀取）
        for (String specialGift : SPECIAL_GIFTS.values()) {
            if (!giftInventory.containsKey(specialGift)) {
                giftInventory.put(specialGift, 0);
            }
        }
    }

    // 核心資源數據
    private int cardCoins = 30;       
    private int giftCoins = 30;       
    private int homeCoins = 30;       
    private int currentEnergy = 20;   
    private int maxEnergy = 40;  

    public GachaManager gachaManager = new GachaManager();
    public List<String> ownedCharacters = new ArrayList<>();
    public List<Partner> myPartners = new ArrayList<>();  // 【修正】全局夥伴列表，保留好感度
    public java.util.Map<String, Integer> giftInventory = new java.util.HashMap<>();
    public java.util.Map<String, Integer> furnitureInventory = new java.util.HashMap<>();
    private ImagePanel imagePanel; // 保存 ImagePanel 的參考

    public final_game() {
        setTitle("遺忘之森");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initGiftInventory(); // ★ 在這裡呼叫初始化
        imagePanel = new ImagePanel();
        setContentPane(imagePanel);
    }

    // 供外部（如 GiftWindow）呼叫的公開存檔方法
    public void saveGame() {
        if (imagePanel != null) {
            imagePanel.saveGame();
        }
    }

    // 圖片面板類別
    class ImagePanel extends JPanel {
        private BufferedImage backgroundImage;
        private List<BufferedImage> characterImages = new ArrayList<>();
        private final String[] characterFiles = {
            "cat.png",       // 0. cat
            "deer.png",      // 1. deer
            "dragon.png",    // 2. dragon
            "elf.png",       // 3. elf
            "fox.png",       // 4. fox
            "owl.png",       // 5. owl
            "player.png",    // 6. player
            "robot_dog.png", // 7. robot_dog
            "sheep.png",     // 8. sheep
            "stone.png"      // 9. stone
        };

        private JButton btnStart, btnContinue, btnRestart, btnHowToPlay, btnAnnouncement;
        private JButton btnMenu, btnBackToMenu, btnSettings, btnIllustrated;
        private DiamondButton btnShop, btnCabin, btnPartner, btnWork, btnMap, btnTBD;
        private JButton btnDemo;

        private final int MENU_WIDTH = 250;     
        private int currentMenuX = 1000;        
        private boolean isMenuOpen = false;     
        private Timer animTimer;                

        private boolean hasSaveData = false;
        private final String SAVE_FILE = "save.dat"; 
        private boolean isPlaying = false; 

        private Clip bgmClip;
        private boolean isBgmOn = true; // 背景音樂開關狀態，預設為 ON

        // 遊戲設定參數
        private int bgmVolume = 80;            
        private int sfxVolume = 70;            
        private String textSizeStr = "中";     
        private String textBoxColorStr = "黑色(白字)";       

        private final int START_WIDTH = 200;
        private final int START_HEIGHT = 60;
        private final int TOP_BTN_WIDTH = 150;
        private final int TOP_BTN_HEIGHT = 50;

        // 背景音樂同步控制
        public void updateBgmVolume() {
            if (bgmClip != null && bgmClip.isOpen()) {
                try {
                    FloatControl gainControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                    if (bgmVolume == 0) {
                        gainControl.setValue(gainControl.getMinimum()); 
                    } else {
                        float db = (float) (Math.log10(bgmVolume / 100.0) * 20.0);
                        db = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), db));
                        gainControl.setValue(db);
                    }
                } catch (Exception ex) {
                    System.err.println("音量調整失敗：" + ex.getMessage());
                }
            }
        }

        private void playBackgroundMusic() {
            try {
                File musicFile = new File(".\\music\\背景音樂.wav");
                if (!musicFile.exists()) {
                    System.out.println("找不到背景音樂檔案！");
                    return;
                }
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
                bgmClip = AudioSystem.getClip();
                bgmClip.open(audioStream);

                updateBgmVolume();

                // ★ 修改點：開場時，依據開關狀態決定是否啟動播放
                if (isBgmOn) {
                    bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                    bgmClip.start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 玩家互動與對話框專用變數
        private Timer randomTalkTimer;     
        private Timer swayTimer;           
        private Timer dialogueHideTimer;   
        private double swayAngle = 0;      
        private int swayTicks = 0;         
        private String currentDialogue = "";
        private Rectangle playerBounds = new Rectangle(0, 0, 0, 0); 
        
        private final String[] playerDialogues = {
            "今天天氣真好呢！", 
            "森林裡似乎有什麼聲音...", 
            "我們要去哪裡探險呢？", 
            "點擊我可以跟我說話喔！", 
            "小夥伴們都準備好了嗎？",
            "這片森林隱藏著古老的秘密...",
            "記得去商店看看有沒有新家具！"
        };

        public ImagePanel() {
            setLayout(null);
            checkSaveData();
            loadBgmState();
            loadImage();
            initButtons();
            initPlayerInteractions();
            playBackgroundMusic();

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateButtonPositions();
                    revalidate();
                    repaint();
                }
            });

            // 能量自動回復：低於 40 時每 10 秒 +1
            Timer energyTimer = new Timer(10000, e -> {
                if (currentEnergy < 40) {
                    currentEnergy = Math.min(40, currentEnergy + 1);
                    repaint(); // 更新 HUD 顯示
                }
            });
            energyTimer.start();
        }

        private void initPlayerInteractions() {
            randomTalkTimer = new Timer(5000, e -> {
                if (isPlaying) {
                    showRandomDialogue();
                }
            });

            dialogueHideTimer = new Timer(3000, e -> {
                currentDialogue = "";
                repaint();
            });
            dialogueHideTimer.setRepeats(false); 

            swayTimer = new Timer(20, e -> {
                swayTicks++;
                swayAngle = Math.sin(swayTicks * 0.4) * 0.08; 
                if (swayTicks >= 100) { 
                    swayTimer.stop();
                    swayAngle = 0; 
                }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isPlaying && playerBounds != null && playerBounds.contains(e.getPoint())) {
                        showRandomDialogue();
                        swayTicks = 0; 
                        swayTimer.start();
                    }
                }
            });
        }

        private void showRandomDialogue() {
            int idx = (int) (Math.random() * playerDialogues.length);
            currentDialogue = playerDialogues[idx];
            dialogueHideTimer.restart(); 
            repaint();
        }

        private void checkSaveData() {
            File saveFile = new File(SAVE_FILE);
            hasSaveData = saveFile.exists(); 
        }
        
        void saveGame() {
            try{
                FileOutputStream fos = new FileOutputStream(SAVE_FILE);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                // 【完整保存】所有遊戲數據
                oos.writeInt(cardCoins);
                oos.writeInt(giftCoins);
                oos.writeInt(homeCoins);
                oos.writeInt(currentEnergy);
                oos.writeInt(maxEnergy);
                oos.writeInt(bgmVolume);
                oos.writeInt(sfxVolume);
                oos.writeObject(textSizeStr);
                oos.writeObject(textBoxColorStr);
                oos.writeObject(ownedCharacters);
                
                // 保存 Partner 完整資訊（name, favor, atk, hp）
                oos.writeInt(myPartners.size());
                for (Partner p : myPartners) {
                    oos.writeObject(p.name);
                    oos.writeInt(p.favor);
                    oos.writeInt(p.atk);
                    oos.writeInt(p.hp);
                }
                
                oos.writeObject(giftInventory);
                oos.writeObject(furnitureInventory);
                oos.writeBoolean(isBgmOn);
                
                oos.close();
                fos.close();
                hasSaveData = true;
                System.out.println("✓ 遊戲已保存！");
            } catch (IOException ex) {
                System.err.println("存檔失敗: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        
        @SuppressWarnings("unchecked")
        private void loadGame() {
            try {
                File saveFile = new File(SAVE_FILE);
                if (!saveFile.exists()) {
                    System.out.println("沒有保存檔案");
                    return;
                }
                
                FileInputStream fis = new FileInputStream(SAVE_FILE);
                ObjectInputStream ois = new ObjectInputStream(fis);
                
                // 【完整讀取】所有遊戲數據
                cardCoins = ois.readInt();
                giftCoins = ois.readInt();
                homeCoins = ois.readInt();
                currentEnergy = ois.readInt();
                maxEnergy = ois.readInt();
                maxEnergy = 40;
                if (currentEnergy > maxEnergy) currentEnergy = maxEnergy;
                bgmVolume = ois.readInt();
                sfxVolume = ois.readInt();
                textSizeStr = (String) ois.readObject();
                textBoxColorStr = (String) ois.readObject();
                ownedCharacters = (List<String>) ois.readObject();
                
                // 重新構建 myPartners 列表（包含 favor, atk, hp）
                myPartners.clear();
                int partnerCount = ois.readInt();
                for (int i = 0; i < partnerCount; i++) {
                    String name = (String) ois.readObject();
                    int favor = ois.readInt();
                    int atk   = ois.readInt();
                    int hp    = ois.readInt();
                    myPartners.add(new Partner(name, ".\\Main_image\\" + getCharacterFileName(name), favor, atk, hp));
                }
                
                giftInventory = (java.util.Map<String, Integer>) ois.readObject();
                furnitureInventory = (java.util.Map<String, Integer>) ois.readObject();
                isBgmOn = ois.readBoolean();
                
                ois.close();
                fis.close();
                hasSaveData = true;
                
                // ★ 新增：讀取檔案完畢後，馬上套用讀取到的音量設定
                updateBgmVolume();

                System.out.println("✓ 遊戲已讀取！");
            } catch (IOException | ClassNotFoundException ex) {
                System.err.println("讀檔失敗: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        
        private void clearSaveGame() {
            File saveFile = new File(SAVE_FILE);
            if (saveFile.exists()) {
                saveFile.delete();
            }
            hasSaveData = false;
        }
        
        private void loadBgmState() {
            try {
                File saveFile = new File(SAVE_FILE);
                if (!saveFile.exists()) {
                    // 沒有保存檔案，使用預設值（true）
                    return;
                }
                
                FileInputStream fis = new FileInputStream(SAVE_FILE);
                ObjectInputStream ois = new ObjectInputStream(fis);
                
                // 跳過所有遊戲數據，直接讀取最後的 isBgmOn
                ois.readInt();  // cardCoins
                ois.readInt();  // giftCoins
                ois.readInt();  // homeCoins
                ois.readInt();  // currentEnergy
                ois.readInt();  // maxEnergy
                ois.readInt();  // bgmVolume
                ois.readInt();  // sfxVolume
                ois.readObject();  // textSizeStr
                ois.readObject();  // textBoxColorStr
                ois.readObject();  // ownedCharacters
                
                // 跳過 myPartners 資訊
                int partnerCount = ois.readInt();
                for (int i = 0; i < partnerCount; i++) {
                    ois.readObject();  // name
                    ois.readInt();     // favor
                    ois.readInt();     // atk
                    ois.readInt();     // hp
                }
                
                ois.readObject();  // giftInventory
                ois.readObject();  // furnitureInventory
                isBgmOn = ois.readBoolean();  // 讀取音樂開關狀態
                
                ois.close();
                fis.close();
            } catch (IOException | ClassNotFoundException ex) {
                System.err.println("讀取音樂狀態失敗: " + ex.getMessage());
                // 發生錯誤時使用預設值
                isBgmOn = true;
            }
        }

        private void loadImage() {
            try {
                File imageFile = new File(".\\Back\\森林.png");
                if (imageFile.exists()) {
                    backgroundImage = ImageIO.read(imageFile);
                }
                File mainImageDir = new File(".\\Main_image\\");
                if (mainImageDir.exists() && mainImageDir.isDirectory()) {
                    for (String fileName : characterFiles) {
                        File charFile = new File(mainImageDir, fileName);
                        if (charFile.exists()) {
                            characterImages.add(ImageIO.read(charFile));
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("加載圖片錯誤: " + e.getMessage());
            }
        }

        private void initButtons() {
            Font btnFont = new Font("標楷體", Font.BOLD, 22);

            btnStart = new GlossyButton("開始遊戲", new Color(240, 160, 0), new Color(255, 240, 150));
            btnContinue = new GlossyButton("繼續遊戲", new Color(40, 180, 80), new Color(150, 255, 150)); 
            btnRestart = new GlossyButton("重新開始", new Color(200, 50, 50), new Color(255, 100, 100)); 

            btnStart.setFont(btnFont);
            btnContinue.setFont(btnFont);
            btnRestart.setFont(btnFont);

            btnHowToPlay = new GlossyButton("遊玩說明", new Color(0, 40, 180), new Color(150, 210, 255));
            btnAnnouncement = new GlossyButton("公 告", new Color(0, 40, 180), new Color(150, 210, 255));
            btnHowToPlay.setFont(btnFont);
            btnAnnouncement.setFont(btnFont);

            btnMenu = new GlossyButton("", new Color(50, 50, 50), new Color(120, 120, 120));
            try {
                BufferedImage iconImg = ImageIO.read(new File(".\\icon\\列表欄.png"));
                Image scaledImg = iconImg.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                btnMenu.setIcon(new ImageIcon(scaledImg));
            } catch (IOException ex) {
                System.err.println("找不到列表欄圖示：" + ex.getMessage());
                btnMenu.setText("☰"); 
                btnMenu.setFont(new Font("Arial", Font.BOLD, 24));
            }

            Font subMenuFont = new Font("標楷體", Font.BOLD, 20);
            btnBackToMenu = new GlossyButton("回開場畫面", new Color(200, 80, 0), new Color(255, 150, 80));
            btnSettings = new GlossyButton("設 定", new Color(0, 120, 150), new Color(100, 200, 255));
            btnIllustrated = new GlossyButton("遊戲圖鑑", new Color(100, 50, 150), new Color(180, 120, 255));
            btnDemo = new GlossyButton("Demo 測試", new Color(150, 30, 30), new Color(255, 120, 120));
            
            btnBackToMenu.setFont(subMenuFont);
            btnSettings.setFont(subMenuFont);
            btnIllustrated.setFont(subMenuFont);
            btnDemo.setFont(subMenuFont);

            btnShop = new DiamondButton("商城");
            btnCabin = new DiamondButton("小屋");
            // 【關鍵修改】：將按鈕設為不可用，這會強制讓它變成灰色且無法點擊
            btnCabin.setEnabled(false); 
            // 如果你有用自定義的設計（例如畫圖出來的按鈕），可以額外加上：
            btnCabin.setToolTipText("功能尚未開放，敬請期待！"); // 滑鼠懸停時提示玩家
            btnPartner = new DiamondButton("夥伴");
            btnWork = new DiamondButton("工作");
            btnMap = new DiamondButton("地圖");
            btnTBD = new DiamondButton("待定");
            btnTBD.setEnabled(false); 

            btnShop.addActionListener(e -> {
                ShopWindow shop = new ShopWindow(final_game.this);
                shop.setVisible(true);
            });
            
            // btnCabin.addActionListener(e -> JOptionPane.showMessageDialog(this, "進入夥伴小屋..."));
            
            btnPartner.addActionListener(e -> {
                // 【修正】只檢查有效的夥伴角色（不包括怪獸）
                List<String> validPartners = new ArrayList<>();
                for (String name : ownedCharacters) {
                    if (final_game.isValidPartner(name)) {
                        validPartners.add(name);
                    }
                }
                
                if (validPartners.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "你還沒有夥伴，快去抽卡吧！");
                } else { 
                    // 檢查新獲得的角色是否已在列表中，如果沒有就添加
                    for (String name : validPartners) {
                        boolean exists = myPartners.stream().anyMatch(p -> p.name.equals(name));
                        if (!exists) {
                            int[] stats = final_game.getBaseStats(name);
                            myPartners.add(new Partner(name, ".\\Main_image\\" + getCharacterFileName(name), 0, stats[0], stats[1]));
                        }
                    }
                    new PartnerWindow(final_game.this, myPartners).setVisible(true);
                }
            });

            btnWork.addActionListener(e -> {
                new WorkGameWindow(final_game.this).setVisible(true);
            });

            btnMap.addActionListener(e -> {
                // 關閉之前的互動提示，改為開啟地圖視窗
                new MapWindow(final_game.this); 
            });

            btnDemo.addActionListener(e -> {
                cardCoins = 10000;
                giftCoins = 10000;
                homeCoins = 10000;
                currentEnergy = 100;
                maxEnergy = 40; 
                repaint();      
                JOptionPane.showMessageDialog(this, "測試成功！資源與能量已最大化重置。");
            });

            btnStart.addActionListener(e -> {
                // 【修正】新遊戲前清空所有數據
                cardCoins = 30;
                giftCoins = 30;
                homeCoins = 30;
                currentEnergy = 20;
                maxEnergy = 40;
                ownedCharacters.clear();
                myPartners.clear();
                giftInventory.clear();
                furnitureInventory.clear();
                
                JOptionPane.showMessageDialog(this, "正在建立新角色，進入遺忘之森...");
                isPlaying = true;         
                isMenuOpen = false;
                final_game.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
                currentMenuX = final_game.this.getWidth(); 
                updateButtonVisibility(); 
                repaint();                
            });

            btnContinue.addActionListener(e -> {
                // 【修正】讀取遊戲進度
                loadGame();
                if (!hasSaveData) {
                    JOptionPane.showMessageDialog(this, "沒有遊戲進度！");
                    return;
                }
                JOptionPane.showMessageDialog(this, "讀取遊戲進度中，歡迎回來！");
                isPlaying = true;         
                isMenuOpen = false;
                final_game.this.setExtendedState(JFrame.MAXIMIZED_BOTH);
                currentMenuX = final_game.this.getWidth(); 
                updateButtonVisibility(); 
                repaint();                
            });

            btnRestart.addActionListener(e -> {
                int result = JOptionPane.showConfirmDialog(this, 
                        "確定要重新開始嗎？\n原本的遊戲進度將會被刪除！", "警告", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    // 【核心修正】：重置所有資源數值回預設值 (20)
                    cardCoins = 30;
                    giftCoins = 30;
                    homeCoins = 30;
                    currentEnergy = 20;
                    maxEnergy = 40;

                    bgmVolume = 80;            
                    sfxVolume = 70;            
                    textSizeStr = "中";     
                    textBoxColorStr = "黑色(白字)";
                    isBgmOn = true;

                    ownedCharacters.clear();
                    myPartners.clear();  // 【修正】重置時也清除全局夥伴列表
                    giftInventory.clear();
                    furnitureInventory.clear();

                    clearSaveGame(); 
                    JOptionPane.showMessageDialog(this, "已重置進度，回到初始主畫面！");
                    isPlaying = false;        
                    isMenuOpen = false;
                    updateButtonVisibility(); 
                    repaint();
                }
            });

            btnHowToPlay.addActionListener(e -> showModalDialog("遊玩說明", getHowToPlayContent()));
            btnAnnouncement.addActionListener(e -> showModalDialog("最新公告", getAnnouncementContent()));

            btnMenu.addActionListener(e -> toggleMenuAnimation());

            btnBackToMenu.addActionListener(e -> {
                // 【修正】返回菜單前保存遊戲
                saveGame();
                isPlaying = false;
                isMenuOpen = false;
                final_game.this.setExtendedState(JFrame.NORMAL);
                final_game.this.setSize(1000, 800);
                final_game.this.setLocationRelativeTo(null);
                currentMenuX = final_game.this.getWidth(); 
                updateButtonVisibility(); 
                repaint();
            });

            btnSettings.addActionListener(e -> showSettingsDialog());
            btnIllustrated.addActionListener(e -> {
                // 檢查是否有夥伴，並將 ownedCharacters 傳入圖鑑
                new IllustratedWindow(final_game.this, ownedCharacters).setVisible(true);
            });

            add(btnStart); add(btnContinue); add(btnRestart); add(btnHowToPlay); add(btnAnnouncement);
            add(btnMenu); add(btnBackToMenu); add(btnSettings); add(btnIllustrated);
            add(btnShop); add(btnCabin); add(btnPartner); add(btnWork); add(btnMap); add(btnTBD);
            add(btnDemo);

            updateButtonVisibility(); 
        }

        // =========================================================
        // ★ 移入的輔助方法群 (徹底解決 cannot find symbol 問題) ★
        // =========================================================
        private void bindSliderAndTextField(JSlider slider, JTextField textField, JDialog dialog) {
            slider.addChangeListener(e -> {
                if (!textField.isFocusOwner()) { 
                    textField.setText(String.valueOf(slider.getValue()));
                }
            });
            ActionListener validateAction = e -> {
                try {
                    int val = Integer.parseInt(textField.getText());
                    if (val < 0 || val > 100) throw new NumberFormatException();
                    slider.setValue(val);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "請輸入 0 到 100 之間的數字！", "數值錯誤", JOptionPane.WARNING_MESSAGE);
                    textField.setText(String.valueOf(slider.getValue()));
                }
            };
            textField.addActionListener(validateAction);
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) { validateAction.actionPerformed(null); }
            });
        }

        private void showModalDialog(String title, String htmlContent) {
            JDialog dialog = new JDialog(final_game.this, title, true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            JLabel contentLabel = new JLabel(htmlContent);
            contentLabel.setFont(new Font("標楷體", Font.PLAIN, 16));
            contentLabel.setVerticalAlignment(SwingConstants.TOP);
            contentLabel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25)); 
            dialog.add(contentLabel);
            dialog.pack(); 
            dialog.setLocationRelativeTo(final_game.this); 
            dialog.setVisible(true); 
        }

        private String getHowToPlayContent() {
            return "<html><body style='width: 420px; font-family: \"標楷體\"; line-height: 1.5;'>"
                 + "<h1 style='color: #2e8b57; text-align: center; padding-bottom: 10px'>【遺忘之森 - 遊玩說明】</h1>"
                 + "<p>歡迎來到《遺忘之森》！在這裡，你將展開一段療癒與淨化的旅程。</p>"
                 + "<h2>⚔️ 核心玩法</h2>"
                 + "<p>一開始你將擁有初始的小夥伴。在森林深處，棲息著因為負面情緒而黑化的怪獸（如影之巨猿、貪婪晶怪等）。與夥伴们攜手擊敗牠們，就能將怪獸淨化，讓牠們轉化為你身邊的新夥伴！</p>"
                 + "<h2>🏠 夥伴小屋與裝飾</h2>"
                 + "<p>擁有新夥伴後，可以在「夥伴小屋」中與牠們互動。你可以賺取金幣，在商店購買「森之呼吸」或「霓虹核心」風格的家具來裝飾空間。</p>"
                 + "<h2>🎁 培養好感度</h2>"
                 + "<p>在商店中購買小禮物或專屬禮物（如貓頭鷹的星空古銅表、火焰狐的不滅炎心石）送給夥伴，提升好感度可大幅增加你們一起討伐怪獸的戰力！</p>"
                 + "<h2>💰 賺取資源</h2>"
                 + "<p>你可以透過遊玩消除卡牌、節奏調頻、修復拼圖等小遊戲來賺取金幣，享受豐富的遊戲體驗。</p>"
                 + "</body></html>";
        }

        private String getAnnouncementContent() {
            return "<html><body style='width: 420px; font-family: \"標楷體\"; line-height: 1.5;'>"
                 + "<h1 style='color: #b22222; text-align: center; padding-bottom: 10px'>【遊戲最新公告】</h1>"
                 + "<p><b>發布日期：2026/05/28</b></p>"
                 + "<hr>"
                 + "<h2>✨ 《遺忘之森》測試版正式啟動！</h2>"
                 + "<p>感謝各位玩家的熱烈支持，目前森林大門已正式開啟。本次測試版本開放以下全新內容：</p>"
                 + "<ol>"
                 + "<li><b>五大黑化怪獸首領降臨</b><br>影之巨猿(憤怒)、鏽蝕海怪(哀傷)、無聲夜裊(孤獨)、貪婪晶怪(慾望)、時鐘亡魂(焦慮)已出現在怪獸地圖中，請各位帶領夥伴前往淨化。</li>"
                 + "<li><b>圖鑑與小屋系統開放</b><br>您可以隨時查看已收集的夥伴，並將獲得的怪獸淨化成新夥伴（例如將影之巨猿淨化為守護猿）。</li>"
                 + "<li><b>家具商城進貨通知</b><br>第一批家具已抵達商店！本次主打「森之呼吸」自然風與「霓虹核心」科技風，歡迎選購以裝飾您的專屬夥伴小屋。</li>"
                 + "</ol>"
                 + "<p>祝各位在遺忘之森中有一段美好的旅途！若遇任何問題請查閱遊玩說明。</p>"
                 + "</body></html>";
        }

        private void showSettingsDialog() {
            JDialog dialog = new JDialog(final_game.this, "遊戲設定與預覽", true);
            // ★ 修改點：視窗高度稍微調大到 700，以容納新按鈕
            dialog.setSize(650, 700); 
            dialog.setLocationRelativeTo(final_game.this);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.getContentPane().setBackground(Color.WHITE);

            JPanel previewContainer = new JPanel(new GridBagLayout());
            previewContainer.setBackground(new Color(245, 246, 250)); 
            previewContainer.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200,200,200)), "故事對話框預覽", 0, 0, new Font("標楷體", Font.BOLD, 16), Color.GRAY));
            
            JPanel roundedTextBox = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(0, 0, 0, 60));
                    g2d.fillRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 20, 20);
                    g2d.setColor(getBackground());
                    g2d.fillRoundRect(0, 0, getWidth() - 16, getHeight() - 16, 20, 20);
                    g2d.dispose();
                }
            };
            roundedTextBox.setOpaque(false);
            roundedTextBox.setPreferredSize(new Dimension(500, 180));
            roundedTextBox.setBorder(BorderFactory.createEmptyBorder(25, 25, 35, 35)); 

            JTextArea previewText = new JTextArea("「在被遺忘的古老森林深處，影之巨猿發出痛苦的咆咆，四周的黑化霧氣開始向外蔓延。大地在它的足跡下崩裂，所有的光芒似乎都正被那無盡的虛無所吞噬...」");
            previewText.setWrapStyleWord(true);
            previewText.setLineWrap(true);
            previewText.setEditable(false);
            previewText.setFocusable(false);
            previewText.setOpaque(false);
            
            roundedTextBox.add(previewText, BorderLayout.CENTER);
            previewContainer.add(roundedTextBox);

            JPanel controlPanel = new JPanel(new GridBagLayout());
            controlPanel.setBackground(Color.WHITE);
            controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(12, 10, 12, 10); // 稍微縮減間距讓排版更緊湊
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;

            Font labelFont = new Font("標楷體", Font.BOLD, 18);

            JLabel lblBgm = new JLabel("音樂音量：");
            lblBgm.setFont(labelFont);
            JSlider sliderBgm = new JSlider(0, 100, bgmVolume);
            sliderBgm.setBackground(Color.WHITE);
            JTextField txtBgm = new JTextField(String.valueOf(bgmVolume), 3);
            txtBgm.setFont(new Font("Arial", Font.PLAIN, 16));
            txtBgm.setHorizontalAlignment(JTextField.CENTER);

            JLabel lblSfx = new JLabel("特效音量：");
            lblSfx.setFont(labelFont);
            JSlider sliderSfx = new JSlider(0, 100, sfxVolume);
            sliderSfx.setBackground(Color.WHITE);
            JTextField txtSfx = new JTextField(String.valueOf(sfxVolume), 3);
            txtSfx.setFont(new Font("Arial", Font.PLAIN, 16));
            txtSfx.setHorizontalAlignment(JTextField.CENTER);

            JLabel lblTextSize = new JLabel("文字大小：");
            lblTextSize.setFont(labelFont);
            String[] sizeOptions = {"小", "中", "大"};
            JComboBox<String> comboSize = new JComboBox<>(sizeOptions);
            comboSize.setFont(new Font("標楷體", Font.PLAIN, 16));
            comboSize.setSelectedItem(textSizeStr);

            JLabel lblBoxColor = new JLabel("文字框色：");
            lblBoxColor.setFont(labelFont);
            String[] colorOptions = {"白色(黑字)", "黑色(白字)", "淡藍色(黑字)", "淡粉色(黑字)"};
            JComboBox<String> comboColor = new JComboBox<>(colorOptions);
            comboColor.setFont(new Font("標楷體", Font.PLAIN, 16));
            comboColor.setSelectedItem(textBoxColorStr);

            // ==========================================
            // ★ 新增：音樂開關按鈕與標籤
            // ==========================================
            JLabel lblBgmToggle = new JLabel("音樂開關：");
            lblBgmToggle.setFont(labelFont);
            
            final boolean[] tempBgmState = { isBgmOn }; // 用陣列儲存彈窗內「暫時」的點擊狀態
            JButton btnBgmToggle = new JButton(tempBgmState[0] ? "ON" : "OFF");
            btnBgmToggle.setFont(new Font("Arial", Font.BOLD, 16));
            btnBgmToggle.setFocusPainted(false);
            btnBgmToggle.setPreferredSize(new Dimension(100, 35));
            // 設定顏色：ON 為深綠色，OFF 為淺灰色
            btnBgmToggle.setBackground(tempBgmState[0] ? new Color(46, 139, 87) : Color.LIGHT_GRAY);
            btnBgmToggle.setForeground(tempBgmState[0] ? Color.WHITE : Color.BLACK);
            
            btnBgmToggle.addActionListener(e -> {
                tempBgmState[0] = !tempBgmState[0];
                btnBgmToggle.setText(tempBgmState[0] ? "ON" : "OFF");
                btnBgmToggle.setBackground(tempBgmState[0] ? new Color(46, 139, 87) : Color.LIGHT_GRAY);
                btnBgmToggle.setForeground(tempBgmState[0] ? Color.WHITE : Color.BLACK);
            });
            // ==========================================

            // 排版佈局加入
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; controlPanel.add(lblBgm, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; controlPanel.add(sliderBgm, gbc);
            gbc.gridx = 2; gbc.weightx = 0; controlPanel.add(txtBgm, gbc);
            
            gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; controlPanel.add(lblSfx, gbc);
            gbc.gridx = 1; gbc.weightx = 1.0; controlPanel.add(sliderSfx, gbc);
            gbc.gridx = 2; gbc.weightx = 0; controlPanel.add(txtSfx, gbc);
            
            gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.gridwidth = 1; controlPanel.add(lblTextSize, gbc);
            gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0; controlPanel.add(comboSize, gbc);
            
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0; controlPanel.add(lblBoxColor, gbc);
            gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0; controlPanel.add(comboColor, gbc);

            // ★ 放入開關按鈕 (靠左對齊，不強行橫向拉寬)
            gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0; controlPanel.add(lblBgmToggle, gbc);
            gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
            controlPanel.add(btnBgmToggle, gbc);

            // 恢復 gbc 的填滿參數設定，防影響後續
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;

            Runnable updatePreview = () -> {
                String sizeSel = (String) comboSize.getSelectedItem();
                String colorSel = (String) comboColor.getSelectedItem();
                int fontSize = 18;
                if ("小".equals(sizeSel)) fontSize = 14;
                else if ("大".equals(sizeSel)) fontSize = 24;
                previewText.setFont(new Font("標楷體", Font.BOLD, fontSize));

                Color bgColor = Color.WHITE;
                Color fgColor = Color.BLACK;
                switch (colorSel) {
                    case "黑色(白字)": bgColor = new Color(40, 40, 40); fgColor = Color.WHITE; break;
                    case "淡藍色(黑字)": bgColor = new Color(200, 230, 255); fgColor = Color.BLACK; break;
                    case "淡粉色(黑字)": bgColor = new Color(255, 210, 220); fgColor = Color.BLACK; break;
                    case "白色(黑字)": default: bgColor = Color.WHITE; fgColor = Color.BLACK; break;
                }
                roundedTextBox.setBackground(bgColor);
                previewText.setForeground(fgColor);
                previewContainer.repaint();
            };

            comboSize.addActionListener(e -> updatePreview.run());
            comboColor.addActionListener(e -> updatePreview.run());
            updatePreview.run(); 

            bindSliderAndTextField(sliderBgm, txtBgm, dialog);
            bindSliderAndTextField(sliderSfx, txtSfx, dialog);

            JButton btnConfirm = new JButton("儲存並套用設定");
            btnConfirm.setFont(new Font("標楷體", Font.BOLD, 20));
            btnConfirm.setBackground(new Color(70, 130, 180));
            btnConfirm.setForeground(Color.WHITE);
            btnConfirm.setFocusPainted(false);
            btnConfirm.setPreferredSize(new Dimension(0, 50));
            btnConfirm.addActionListener(e -> {
                bgmVolume = sliderBgm.getValue();
                sfxVolume = sliderSfx.getValue();
                textSizeStr = (String) comboSize.getSelectedItem();
                textBoxColorStr = (String) comboColor.getSelectedItem();
                
                // ★ 修改點：判斷並執行音樂的一時停止與接續播放
                boolean oldBgmState = isBgmOn;
                isBgmOn = tempBgmState[0];
                
                if (bgmClip != null && bgmClip.isOpen()) {
                    if (isBgmOn && !oldBgmState) {
                        // 從 OFF 切換到 ON：重新綁定循環，並「從原本暫停的位置」繼續播放
                        bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                        bgmClip.start();
                    } else if (!isBgmOn && oldBgmState) {
                        // 從 ON 切換到 OFF：暫停音樂，保留當前進度
                        bgmClip.stop();
                    }
                }
                
                updateBgmVolume();
                
                if (isPlaying) {
                    saveGame();
                }
                
                dialog.dispose(); 
                JOptionPane.showMessageDialog(final_game.this, "遊戲設定已完美更新！");
            });

            JPanel bottomWrapper = new JPanel(new BorderLayout());
            bottomWrapper.add(controlPanel, BorderLayout.CENTER);
            bottomWrapper.add(btnConfirm, BorderLayout.SOUTH);

            dialog.add(previewContainer, BorderLayout.CENTER);
            dialog.add(bottomWrapper, BorderLayout.SOUTH);
            
            dialog.setVisible(true);
        }

        private void toggleMenuAnimation() {
            if (animTimer != null && animTimer.isRunning()) return; 
            int targetX = isMenuOpen ? getWidth() : getWidth() - MENU_WIDTH;
            isMenuOpen = !isMenuOpen;
            if (isMenuOpen) {
                btnBackToMenu.setVisible(true);
                btnSettings.setVisible(true);
                btnIllustrated.setVisible(true);
                btnDemo.setVisible(true); 
            }
            animTimer = new Timer(10, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isMenuOpen) {
                        currentMenuX -= 20;
                        if (currentMenuX <= targetX) {
                            currentMenuX = targetX;
                            animTimer.stop();
                        }
                    } else {
                        currentMenuX += 20;
                        if (currentMenuX >= targetX) {
                            currentMenuX = targetX;
                            animTimer.stop();
                            btnBackToMenu.setVisible(false);
                            btnSettings.setVisible(false);
                            btnIllustrated.setVisible(false);
                            btnDemo.setVisible(false); 
                        }
                    }
                    int yOffset = 120;
                    btnBackToMenu.setBounds(currentMenuX + 25, yOffset, 200, 50);
                    btnSettings.setBounds(currentMenuX + 25, yOffset + 70, 200, 50);
                    btnIllustrated.setBounds(currentMenuX + 25, yOffset + 140, 200, 50);
                    btnDemo.setBounds(currentMenuX + 25, yOffset + 210, 200, 50); 
                    repaint(); 
                }
            });
            animTimer.start();
        }

        private void updateButtonVisibility() {
            if (isPlaying) {
                btnStart.setVisible(false);
                btnContinue.setVisible(false);
                btnRestart.setVisible(false);
                btnHowToPlay.setVisible(false);
                btnAnnouncement.setVisible(false);
                
                btnMenu.setVisible(true);
                btnBackToMenu.setVisible(isMenuOpen);
                btnSettings.setVisible(isMenuOpen);
                btnIllustrated.setVisible(isMenuOpen);
                btnDemo.setVisible(isMenuOpen); 

                btnShop.setVisible(true);
                btnCabin.setVisible(true);
                btnPartner.setVisible(true);
                btnWork.setVisible(true);
                btnMap.setVisible(true);
                btnTBD.setVisible(true);
                
                if (randomTalkTimer != null && !randomTalkTimer.isRunning()) {
                    randomTalkTimer.start();
                }
            } else {
                btnMenu.setVisible(false);
                btnBackToMenu.setVisible(false);
                btnSettings.setVisible(false);
                btnIllustrated.setVisible(false);
                btnDemo.setVisible(false); 
                
                btnShop.setVisible(false);
                btnCabin.setVisible(false);
                btnPartner.setVisible(false);
                btnWork.setVisible(false);
                btnMap.setVisible(false);
                btnTBD.setVisible(false);

                btnHowToPlay.setVisible(true);
                btnAnnouncement.setVisible(true);
                if (hasSaveData) {
                    btnStart.setVisible(false);
                    btnContinue.setVisible(true);
                    btnRestart.setVisible(true);
                } else {
                    btnStart.setVisible(true);
                    btnContinue.setVisible(false);
                    btnRestart.setVisible(false);
                }
                
                if (randomTalkTimer != null) randomTalkTimer.stop();
                if (swayTimer != null) swayTimer.stop();
                currentDialogue = "";
                swayAngle = 0;
            }
            updateButtonPositions(); 
        }

        private void updateButtonPositions() {
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            if (panelWidth == 0 || panelHeight == 0) return;

            if (animTimer == null || !animTimer.isRunning()) {
                currentMenuX = isMenuOpen ? (panelWidth - MENU_WIDTH) : panelWidth;
            }

            if (!isPlaying) {
                int baseY = panelHeight - START_HEIGHT - 60;
                if (!hasSaveData) {
                    int startX = (panelWidth - START_WIDTH) / 2;
                    btnStart.setBounds(startX, baseY, START_WIDTH, START_HEIGHT);
                } else {
                    int gap = 40; 
                    int totalWidth = (START_WIDTH * 2) + gap;
                    int leftX = (panelWidth - totalWidth) / 2;
                    btnContinue.setBounds(leftX, baseY, START_WIDTH, START_HEIGHT);
                    btnRestart.setBounds(leftX + START_WIDTH + gap, baseY, START_WIDTH, START_HEIGHT);
                }
                int topRightX = panelWidth - TOP_BTN_WIDTH - 50;
                btnHowToPlay.setBounds(topRightX, 30, TOP_BTN_WIDTH, TOP_BTN_HEIGHT);
                btnAnnouncement.setBounds(topRightX, 30 + TOP_BTN_HEIGHT + 20, TOP_BTN_WIDTH, TOP_BTN_HEIGHT);
            } else {
                btnMenu.setBounds(panelWidth - 80, 30, 50, 50);
                int yOffset = 120;
                btnBackToMenu.setBounds(currentMenuX + 25, yOffset, 200, 50);
                btnSettings.setBounds(currentMenuX + 25, yOffset + 70, 200, 50);
                btnIllustrated.setBounds(currentMenuX + 25, yOffset + 140, 200, 50);
                
                btnDemo.setBounds(currentMenuX + 25, yOffset + 210, 200, 50);

                int diamondSize = 120; 
                int offsetX = 70; 
                int deltaY = 70; 
                
                int centerX = 110; 
                int totalHeight = (3 * deltaY) + diamondSize;
                int startY = panelHeight - totalHeight - 50; 

                btnShop.setBounds(centerX, startY, diamondSize, diamondSize);
                btnCabin.setBounds(centerX - offsetX, startY + deltaY, diamondSize, diamondSize);
                btnPartner.setBounds(centerX + offsetX, startY + deltaY, diamondSize, diamondSize);
                btnWork.setBounds(centerX, startY + 2 * deltaY, diamondSize, diamondSize);
                btnTBD.setBounds(centerX - offsetX, startY + 3 * deltaY, diamondSize, diamondSize);
                btnMap.setBounds(centerX + offsetX, startY + 3 * deltaY, diamondSize, diamondSize);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            int width = getWidth();
            int height = getHeight();

            if (backgroundImage != null) {
                g2d.drawImage(backgroundImage, 0, 0, width, height, this);
            } else {
                g2d.setColor(new Color(100, 150, 100));
                g2d.fillRect(0, 0, width, height);
            }

            if (!isPlaying) {
                drawCharacters(g2d);
                drawGameTitle(g2d);
            } else {
                drawGamePlayScreen(g2d);
                
                if (currentMenuX < width) {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(0, 0, 0, 102)); 
                    g2d.fillRect(currentMenuX, 0, MENU_WIDTH, height);
                    g2d.setColor(new Color(255, 255, 255, 40));
                    g2d.drawLine(currentMenuX, 0, currentMenuX, height);
                }
            }
        }

        private void drawGamePlayScreen(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            int hudX = 30;
            int hudY = 30;
            int hudW = 230;
            int hudH = 165;

            g2d.setColor(new Color(15, 20, 35, 180));
            g2d.fillRoundRect(hudX, hudY, hudW, hudH, 15, 15);
            g2d.setColor(new Color(180, 210, 255, 120));
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawRoundRect(hudX, hudY, hudW, hudH, 15, 15);
            g2d.setColor(new Color(100, 150, 255, 60));
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRoundRect(hudX + 4, hudY + 4, hudW - 8, hudH - 8, 11, 11);

            g2d.setFont(new Font("標楷體", Font.BOLD, 18));
            g2d.setColor(Color.WHITE);
            g2d.drawString("🪙 卡幣：" + cardCoins, hudX + 20, hudY + 35);
            g2d.drawString("🎁 禮幣：" + giftCoins, hudX + 20, hudY + 70);
            g2d.drawString("🏠 家幣：" + homeCoins, hudX + 20, hudY + 105);
            g2d.drawString("⚡ 能量：" + currentEnergy + "/" + maxEnergy, hudX + 20, hudY + 140);

            BufferedImage playerImg = null;
            for (int i = 0; i < characterFiles.length; i++) {
                if (characterFiles[i].equals("player.png") && i < characterImages.size()) {
                    playerImg = characterImages.get(i);
                    break;
                }
            }

            if (playerImg != null) {
                // 將 0.7 改為 0.4 (縮小玩家圖片大小)
                int pWidth = (int) (playerImg.getWidth() * 0.4);
                int pHeight = (int) (playerImg.getHeight() * 0.4);
                int pX = (getWidth() - pWidth) / 2;
                int pY = (getHeight() - pHeight) / 2 + 50; 
                
                playerBounds.setBounds(pX, pY, pWidth, pHeight);

                AffineTransform oldTx = g2d.getTransform();
                g2d.rotate(swayAngle, pX + pWidth / 2, pY + pHeight);
                g2d.drawImage(playerImg, pX, pY, pWidth, pHeight, null);
                g2d.setTransform(oldTx);

                if (!currentDialogue.isEmpty()) {
                    g2d.setFont(new Font("標楷體", Font.BOLD, 22));
                    FontMetrics fm = g2d.getFontMetrics();
                    int textW = fm.stringWidth(currentDialogue) + 40; 
                    int textH = 50;
                    int boxX = pX + pWidth / 2 - textW / 2; 
                    int boxY = pY - textH - 25; 

                    RoundRectangle2D.Float bubbleBody = new RoundRectangle2D.Float(boxX, boxY, textW, textH, 20, 20);
                    Polygon tail = new Polygon(
                        new int[]{pX + pWidth / 2 - 12, pX + pWidth / 2 + 12, pX + pWidth / 2}, 
                        new int[]{boxY + textH - 5, boxY + textH - 5, boxY + textH + 15},       
                        3
                    );
                    
                    Area bubble = new Area(bubbleBody);
                    bubble.add(new Area(tail));

                    g2d.setColor(new Color(255, 255, 255, 230));
                    g2d.fill(bubble);
                    
                    g2d.setColor(new Color(150, 150, 150));
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.draw(bubble);

                    g2d.setColor(Color.BLACK);
                    g2d.drawString(currentDialogue, boxX + 20, boxY + 33);
                }
            }
        }

        private void drawCharacters(Graphics2D g2d) {
            if (characterImages.isEmpty()) return;
            int cx = getWidth() / 2;
            int cy = getHeight() / 2 - 80; 
            float[][] layout = {
                { -80, -140, 0.17f}, // 0. cat
                {-130,   -5, 0.20f}, // 1. deer
                { 250, -120, 0.21f}, // 2. dragon
                { 300,   20, 0.20f}, // 3. elf
                {-170,  120, 0.37f}, // 4. fox
                { 140,   20, 0.19f}, // 5. owl
                {   0,   60, 0.32f}, // 6. player
                { 230,  160, 0.20f}, // 7. robot_dog
                {  60,  180, 0.18f}, // 8. sheep
                {-250,  180, 0.13f}  // 9. stone
            };
            for (int i = 0; i < characterImages.size(); i++) {
                BufferedImage img = characterImages.get(i);
                float xOffset = (i < layout.length) ? layout[i][0] : (i * 50);
                float yOffset = (i < layout.length) ? layout[i][1] : (i * 20);
                float scale   = (i < layout.length) ? layout[i][2] : 0.3f;
                int drawWidth = (int) (img.getWidth() * scale);
                int drawHeight = (int) (img.getHeight() * scale);
                int drawX = cx + (int)xOffset - (drawWidth / 2);
                int drawY = cy + (int)yOffset - (drawHeight / 2);
                g2d.drawImage(img, drawX, drawY, drawWidth, drawHeight, null);
            }
        }

        private void drawGameTitle(Graphics2D g2d) {
            String text = "遺忘之森";
            Font titleFont = new Font("標楷體", Font.BOLD, 120); 
            g2d.setFont(titleFont);
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2; 
            int baseY = getHeight() - START_HEIGHT - 60;
            int y = baseY - 50;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(text, x + 8, y + 8);
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.drawString(text, x + 12, y + 12);
            GlyphVector gv = titleFont.createGlyphVector(g2d.getFontRenderContext(), text);
            Shape shape = gv.getOutline(x, y);
            g2d.setStroke(new BasicStroke(15));
            g2d.setColor(new Color(80, 40, 120)); 
            g2d.draw(shape);
            g2d.setStroke(new BasicStroke(8));
            g2d.setColor(new Color(120, 80, 160));
            g2d.draw(shape);
            GradientPaint gp = new GradientPaint(x, y - 60, new Color(100, 240, 255), 
                                                 x, y + 20, new Color(80, 40, 200), true);
            g2d.setPaint(gp);
            g2d.fill(shape);
            g2d.setStroke(new BasicStroke(2));
        }

        private void showShopDialog() {
            JDialog dialog = new JDialog(final_game.this, "商城", true);
            dialog.setSize(850, 600); // 視窗放大
            dialog.setLocationRelativeTo(final_game.this);
            dialog.setLayout(new GridLayout(1, 3, 20, 0)); // 增加間距
            dialog.getContentPane().setBackground(new Color(245, 245, 245));

            String[] titles = {"抽卡", "禮物", "家具"};
            String[] imgPaths = {
                ".\\Main_image\\owl.png", 
                ".\\Gift_image\\精緻小紙袋.png", 
                ".\\Furniture_image\\翠玉巨葉床.png"
            };

            for (int i = 0; i < 3; i++) {
                final int index = i;
                OvalButton btn = new OvalButton(titles[i], imgPaths[i]);
                btn.addActionListener(e -> JOptionPane.showMessageDialog(dialog, "進入 " + titles[index] + " 區塊..."));
                dialog.add(btn);
            }
            dialog.setVisible(true);
        }
    }

    // 自訂菱形按鈕類別，包含菱形外觀的繪製邏輯與點擊區域判斷
    class DiamondButton extends JButton {
        
        public DiamondButton(String text) {
            super(text);
            setContentAreaFilled(false); 
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("標楷體", Font.BOLD, 28)); 
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();

            Polygon outerDiamond = new Polygon(
                new int[]{w/2, w-2, w/2, 2},
                new int[]{2, h/2, h-2, h/2},
                4
            );

            if (!isEnabled()) {
                g2d.setColor(new Color(100, 100, 100, 200)); 
            } else if (getModel().isRollover()) {
                g2d.setColor(new Color(60, 120, 200, 220));  
            } else {
                g2d.setColor(new Color(40, 45, 60, 220));    
            }
            g2d.fill(outerDiamond);

            if (!isEnabled()) {
                g2d.setColor(new Color(150, 150, 150));      
            } else {
                g2d.setColor(new Color(200, 220, 255));      
            }
            g2d.setStroke(new BasicStroke(3f));
            g2d.draw(outerDiamond);

            int inset = 8; 
            Polygon innerDiamond = new Polygon(
                new int[]{w/2, w-2-inset, w/2, 2+inset},
                new int[]{2+inset, h/2, h-2-inset, h/2},
                4
            );
            g2d.setStroke(new BasicStroke(1f));
            g2d.draw(innerDiamond);

            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(getText());
            int textHeight = fm.getAscent();
            
            if (!isEnabled()) {
                g2d.setColor(new Color(0, 0, 0, 100));       
            } else {
                g2d.setColor(new Color(0, 0, 0, 150));
            }
            g2d.drawString(getText(), (w - textWidth)/2 + 2, (h + textHeight)/2 - 3);
            
            if (!isEnabled()) {
                g2d.setColor(new Color(180, 180, 180));      
            } else {
                g2d.setColor(getForeground());               
            }
            g2d.drawString(getText(), (w - textWidth)/2, (h + textHeight)/2 - 5);

            g2d.dispose();
        }

        @Override
        public boolean contains(int x, int y) {
            int w = getWidth();
            int h = getHeight();
            Polygon p = new Polygon(
                new int[]{w/2, w, w/2, 0},
                new int[]{0, h/2, h, h/2},
                4
            );
            return p.contains(x, y);
        }
    }

    // 自訂光澤按鈕類別，包含漸層底色與玻璃高光效果的繪製邏輯
    class GlossyButton extends JButton {
        private Color baseColor;
        private Color highlightColor;

        public GlossyButton(String text, Color baseColor, Color highlightColor) {
            super(text);
            this.baseColor = baseColor;
            this.highlightColor = highlightColor;
            setContentAreaFilled(false); 
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setForeground(Color.WHITE);  
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            GradientPaint bgGradient = new GradientPaint(0, 0, highlightColor, 0, h, baseColor);
            g2d.setPaint(bgGradient);
            g2d.fillRoundRect(0, 0, w, h, h, h); 
            g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, h, h));
            GradientPaint glassGradient = new GradientPaint(0, 0, new Color(255, 255, 255, 220),
                                                           0, h / 2, new Color(255, 255, 255, 30));
            g2d.setPaint(glassGradient);
            g2d.fillOval(-w / 4, -h / 2, w + w / 2, (int)(h * 1.1));
            g2d.setClip(null);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.setColor(baseColor.darker());
            g2d.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            Icon icon = getIcon();
            if (icon != null) {
                int iconX = (w - icon.getIconWidth()) / 2;
                int iconY = (h - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2d, iconX, iconY);
            } else {
                FontMetrics fm = g2d.getFontMetrics();
                int stringWidth = fm.stringWidth(getText());
                int stringHeight = fm.getAscent();
                int x = (w - stringWidth) / 2;
                int y = (h + stringHeight) / 2 - 4;
                g2d.setColor(new Color(0, 0, 0, 120));
                g2d.drawString(getText(), x + 1, y + 2);
                g2d.setColor(getForeground());
                g2d.drawString(getText(), x, y);
            }
            g2d.dispose();
        }
    }

    // 主方法，啟動遊戲應用程式
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final_game app = new final_game();
            app.setVisible(true);
        });
    }

    // 自訂橢圓按鈕類別，包含圖片與文字的繪製邏輯
    class OvalButton extends JButton {
        private BufferedImage iconImg;
        private boolean isHovered = false;

        public OvalButton(String text, String imgPath) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            try {
                iconImg = ImageIO.read(new File(imgPath));
            } catch (IOException e) { System.err.println("無法載入圖示: " + imgPath); }

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { isHovered = true; repaint(); }
                public void mouseExited(java.awt.event.MouseEvent e) { isHovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 讓橢圓整體稍微縮小一點 (在中心繪製)
            int padding = 20;
            int w = getWidth() - padding * 2, h = getHeight() - padding * 2;
            int x = padding, y = padding;
            
            g2d.setColor(isHovered ? new Color(255, 255, 255, 230) : new Color(230, 230, 230, 200));
            g2d.fillOval(x, y, w, h);
            
            // 繪製圖片
            if (iconImg != null) {
                float alpha = isHovered ? 0.6f : 0.9f; 
                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
                int imgW = (int)(w * 0.75); // 圖片縮小一點，騰出空間給大字體
                int imgH = (int)(h * 0.75);
                g2d.drawImage(iconImg, x + (w - imgW) / 2, y + (h - imgH) / 2 - 30, imgW, imgH, null);
            }

            // 繪製文字 (放大、陰影)
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
            g2d.setFont(new Font("標楷體", Font.BOLD, 40)); // 字體放大到 40
            FontMetrics fm = g2d.getFontMetrics();
            String text = getText();
            
            for (int i = 0; i < text.length(); i++) {
                String charStr = String.valueOf(text.charAt(i));
                int charW = fm.stringWidth(charStr);
                int posX = (getWidth() - charW) / 2;
                int posY = (h / 2) + 20 + (i * 45); // 增加間距
                
                // 繪製文字陰影 (灰色)
                g2d.setColor(new Color(150, 150, 150, 150));
                g2d.drawString(charStr, posX + 2, posY + 2);
                
                // 繪製主文字
                g2d.setColor(Color.BLACK);
                g2d.drawString(charStr, posX, posY);
            }
            g2d.dispose();
        }
    }

    // 商店商品類別，包含一般商品與專屬對象/風格的完整建構子
    class ShopItem {
        String name, imgPath, statLabel, targetChar, style;
        int price, statValue;
        
        // 一般商品的建構子
        public ShopItem(String n, String p, int pr, String sl, int sv) {
            this(n, p, pr, sl, sv, null, null);
        }
        
        // 包含專屬對象與風格的完整建構子
        public ShopItem(String n, String p, int pr, String sl, int sv, String targetChar, String style) {
            name = n; imgPath = p; price = pr; statLabel = sl; statValue = sv; 
            this.targetChar = targetChar;
            this.style = style;
        }
    }

    // 獨立的商城視窗類別
    class ShopWindow extends JDialog {
        private CardDisplayArea displayArea;

        public ShopWindow(Frame parent) {
            super(parent, "商店", true);
            setSize(1000, 750);
            setLocationRelativeTo(parent);
            
            JTabbedPane tabs = new JTabbedPane();
            displayArea = new CardDisplayArea();

            tabs.add("抽卡", createCardPanel());      // 已補上方法
            tabs.add("禮物", createItemPanel("gift"));
            tabs.add("家具", createItemPanel("furniture"));
            
            add(tabs);
        }

        // 建立商品面板的通用方法
        private JComponent createItemPanel(String type) {
            JPanel panel = new JPanel(new GridLayout(0, 2, 25, 25)); 
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            if (type.equals("gift")) {
                panel.add(new ItemCard(new ShopItem("小禮物", ".\\Gift_image\\精緻小紙袋.png", 20, "好感度", 3), final_game.this));
                panel.add(new ItemCard(new ShopItem("大禮物", ".\\Gift_image\\彩色禮物盒.png", 200, "好感度", 5), final_game.this));

                for (String charName : SPECIAL_GIFTS.keySet()) {
                    String giftName = SPECIAL_GIFTS.get(charName);
                    String imgFileName = giftName + ".png";
                    panel.add(new ItemCard(new ShopItem(giftName, ".\\Gift_image\\" + imgFileName, 500, "好感度", 10, charName, null), final_game.this));
                }
            } else if (type.equals("furniture")) {
                // 森之呼吸系列 (傳入風格標籤)
                String style1 = "森之呼吸";
                panel.add(new ItemCard(new ShopItem("翠玉巨葉床", ".\\Furniture_image\\森之呼吸\\翠玉巨葉床.png", 300, "舒適度", 10, null, style1), final_game.this));
                panel.add(new ItemCard(new ShopItem("原木年輪桌", ".\\Furniture_image\\森之呼吸\\原木年輪桌.png", 400, "舒適度", 15, null, style1), final_game.this));
                panel.add(new ItemCard(new ShopItem("胖胖蘑菇椅", ".\\Furniture_image\\森之呼吸\\胖胖蘑菇椅.png", 500, "舒適度", 20, null, style1), final_game.this));
                panel.add(new ItemCard(new ShopItem("蔓生綠藤架", ".\\Furniture_image\\森之呼吸\\蔓生綠藤架.png", 600, "舒適度", 25, null, style1), final_game.this));
                panel.add(new ItemCard(new ShopItem("軟綿青苔毯", ".\\Furniture_image\\森之呼吸\\軟綿青苔毯.png", 700, "舒適度", 30, null, style1), final_game.this));
                panel.add(new ItemCard(new ShopItem("森之呼吸背景", ".\\Furniture_image\\森之呼吸\\森之呼吸背景.png", 900, "舒適度", 50, null, style1), final_game.this));

                // 霓虹核心系列 (傳入風格標籤)
                String style2 = "霓虹核心";
                panel.add(new ItemCard(new ShopItem("懸浮膠囊床", ".\\Furniture_image\\霓虹核心\\懸浮膠囊床.png", 350, "舒適度", 12, null, style2), final_game.this));
                panel.add(new ItemCard(new ShopItem("藍光金屬桌", ".\\Furniture_image\\霓虹核心\\藍光金屬桌.png", 450, "舒適度", 18, null, style2), final_game.this));
                panel.add(new ItemCard(new ShopItem("銀白幾何椅", ".\\Furniture_image\\霓虹核心\\銀白幾何椅.png", 550, "舒適度", 22, null, style2), final_game.this));
                panel.add(new ItemCard(new ShopItem("霓虹儲物槽", ".\\Furniture_image\\霓虹核心\\霓虹儲物槽.png", 650, "舒適度", 28, null, style2), final_game.this));
                panel.add(new ItemCard(new ShopItem("銀網電路毯", ".\\Furniture_image\\霓虹核心\\銀網電路毯.png", 750, "舒適度", 35, null, style2), final_game.this));
                panel.add(new ItemCard(new ShopItem("霓虹核心背景", ".\\Furniture_image\\霓虹核心\\霓虹核心背景.png", 950, "舒適度", 55, null, style2), final_game.this));
            }
            
            // 防變形機制
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(panel, BorderLayout.NORTH);

            JScrollPane scrollPane = new JScrollPane(wrapper);
            scrollPane.getVerticalScrollBar().setUnitIncrement(20); 
            scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
            
            return scrollPane;
        }

        private JPanel createCardPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            // displayArea 已經在 ShopWindow 建構子中初始化了，這裡直接用
            panel.add(displayArea, BorderLayout.CENTER); // 將顯示區加入中間
            
            JPanel btnPanel = new JPanel();
            JButton btnSingle = new JButton("單抽 (20卡幣)");
            JButton btnTen = new JButton("十連抽 (90卡幣)");
            
            // 按鈕只負責呼叫 performGacha，動畫由 performGacha 控制
            btnSingle.addActionListener(e -> performGacha(1, 20));
            btnTen.addActionListener(e -> performGacha(10, 90));
            
            btnPanel.add(btnSingle); btnPanel.add(btnTen);
            panel.add(btnPanel, BorderLayout.SOUTH);
            return panel;
        }

        private void performGacha(int count, int cost) {
            if (cardCoins < cost) {
                JOptionPane.showMessageDialog(this, "卡幣不足！");
                return;
            }
            cardCoins -= cost;
            String[] results = final_game.this.gachaManager.pull(count, count == 10);
            
            // 建立一個專屬的抽卡結果彈出視窗
            JDialog resultDialog = new JDialog(this, "★ 抽卡結果 ★", true);
            resultDialog.setSize(count == 10 ? 960 : 320, count == 10 ? 620 : 380);
            resultDialog.setLocationRelativeTo(this);
            resultDialog.setLayout(new BorderLayout());
            resultDialog.getContentPane().setBackground(new Color(20, 20, 20)); // 神祕高級感暗色底

            // 建立結果顯示面板
            JPanel resultPanel = new JPanel();
            resultPanel.setOpaque(false); 
            
            if (count == 10) {
                // 10連抽：5個一排，共2排，加寬間距
                resultPanel.setLayout(new GridLayout(2, 5, 20, 20));
            } else {
                // 1抽：顯示於視窗正中間
                resultPanel.setLayout(new GridBagLayout()); 
            }
            resultPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

            for (String res : results) {
                // 取得對應等級的顏色
                Color rarityColor = final_game.getRarityColor(res);
                
                // ★ 改寫：使用匿名內部類別自定義繪製「外圓角、內八角」的特殊面板
                JPanel cardBox = new JPanel(new BorderLayout(5, 5)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        // 開啟抗鋸齒，讓邊緣平滑
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        int w = getWidth();
                        int h = getHeight();
                        int t = 10;   // 邊框的厚度
                        int cut = 28; // 內八角的四角斜切起點大小
                        int arc = 20; // 外框的圓角弧度
                        
                        // 1. 建立內八角的形狀
                        Polygon inner = new Polygon();
                        inner.addPoint(cut, t);             // 上緣左點
                        inner.addPoint(w - cut, t);         // 上緣右點
                        inner.addPoint(w - t, cut);         // 右緣上點
                        inner.addPoint(w - t, h - cut);     // 右緣下點
                        inner.addPoint(w - cut, h - t);     // 下緣右點
                        inner.addPoint(cut, h - t);         // 下緣左點
                        inner.addPoint(t, h - cut);         // 左緣下點
                        inner.addPoint(t, cut);             // 左緣上點
                        
                        // 填滿內八角的白色底
                        g2d.setColor(Color.WHITE);
                        g2d.fill(inner);
                        
                        // 2. 建立外側圓角矩形，並與內八角做「相減」取得專屬邊框 Area
                        java.awt.geom.Area outerArea = new java.awt.geom.Area(
                            new java.awt.geom.RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc)
                        );
                        outerArea.subtract(new java.awt.geom.Area(inner));
                        
                        // 畫上等級對應的邊框顏色
                        g2d.setColor(rarityColor);
                        g2d.fill(outerArea);
                        
                        g2d.dispose();
                    }
                };
                cardBox.setOpaque(false); // 必須設為透明，才不會擋住後方深色背景
                cardBox.setPreferredSize(new Dimension(170, 240)); 
                // 利用 EmptyBorder 往內推擠組件，確保文字與圖片不會超線跑到邊框上
                cardBox.setBorder(BorderFactory.createEmptyBorder(22, 10, 15, 10)); 
                
                // 上方顯示圖案
                String imgPath = ".\\Main_image\\" + getCharacterFileName(res);
                JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
                try {
                    BufferedImage img = ImageIO.read(new File(imgPath));
                    imgLabel.setIcon(new ImageIcon(img.getScaledInstance(120, 120, Image.SCALE_SMOOTH))); 
                } catch (Exception e) {
                    imgLabel.setText("?");
                    imgLabel.setFont(new Font("Arial", Font.BOLD, 50));
                }
                cardBox.add(imgLabel, BorderLayout.CENTER);

                // 處理是否 New!! 與重複邏輯，下方顯示夥伴名稱
                boolean isDup = final_game.this.ownedCharacters.contains(res);
                String status = isDup ? "(重)" : "(New!)";
                JLabel nameLabel = new JLabel(res + " " + status, SwingConstants.CENTER);
                nameLabel.setFont(new Font("標楷體", Font.BOLD, 18));
                
                // New! 字樣特別上色區別
                if (!isDup) {
                    nameLabel.setForeground(new Color(220, 20, 60)); // 紅色 New!
                } else {
                    nameLabel.setForeground(Color.DARK_GRAY);
                }
                
                if (isDup) {
                    cardCoins += 5;
                } else {
                    final_game.this.ownedCharacters.add(res);
                    
                    int[] stats = final_game.getBaseStats(res);
                    final_game.this.myPartners.add(new Partner(res, ".\\Main_image\\" + getCharacterFileName(res), 0, stats[0], stats[1]));
                }
                cardBox.add(nameLabel, BorderLayout.SOUTH);
                
                resultPanel.add(cardBox);
            }

            resultDialog.add(resultPanel, BorderLayout.CENTER);

            // 下方多加一個「確認收下」按鈕
            JButton btnClose = new JButton("確認收下");
            btnClose.setFont(new Font("標楷體", Font.BOLD, 18));
            btnClose.setBackground(new Color(70, 130, 180));
            btnClose.setForeground(Color.WHITE);
            btnClose.setFocusPainted(false);
            btnClose.setPreferredSize(new Dimension(150, 40));
            btnClose.addActionListener(e -> resultDialog.dispose());

            JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            southPanel.setOpaque(false);
            southPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            southPanel.add(btnClose);
            resultDialog.add(southPanel, BorderLayout.SOUTH);

            // 顯示大視窗
            resultDialog.setVisible(true);
            
            displayArea.updateCards(results); // 更新主商店底下的滾動欄位
            repaint();
        }
    }

    // 獨立的商品卡片類別 (外大直框設計)
    class ItemCard extends JPanel {
        public ItemCard(ShopItem item, Frame parent) {
            setLayout(new GridLayout(1, 2, 10, 0));
            setPreferredSize(new Dimension(420, 180)); 
            setBackground(new Color(250, 250, 250)); 
            
            // 外大直框樣式
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 150), 3), 
                BorderFactory.createEmptyBorder(15, 10, 15, 10) 
            ));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // 點擊事件：開啟購買頁面
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    PurchaseDialog pd = new PurchaseDialog(parent, item, (final_game) parent);
                    pd.setVisible(true);
                }
            });

            // 左半部：圖片區
            JLabel imgLabel = new JLabel();
            try {
                BufferedImage img = ImageIO.read(new File(item.imgPath));
                imgLabel.setIcon(new ImageIcon(img.getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
            } catch (Exception e) { 
                imgLabel.setText("圖片載入失敗");
            }
            imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            // 右半部：文字資訊區
            JPanel infoPanel = new JPanel(new GridLayout(0, 1, 0, 5)); 
            infoPanel.setOpaque(false);
            
            Font textFont = new Font("標楷體", Font.BOLD, 22); 
            JLabel nameLabel = new JLabel(item.name, SwingConstants.CENTER);
            nameLabel.setFont(textFont);
            infoPanel.add(nameLabel);
            
            // ★ 新增：判斷並加入特殊標示 (專屬對象 OR 家具風格)
            if (item.targetChar != null && !item.targetChar.isEmpty()) {
                JLabel targetLabel = new JLabel("(專屬: " + item.targetChar + ")", SwingConstants.CENTER);
                targetLabel.setFont(new Font("標楷體", Font.BOLD, 16));
                targetLabel.setForeground(new Color(160, 60, 180)); // 醒目的紫色
                infoPanel.add(targetLabel);
            } else if (item.style != null && !item.style.isEmpty()) {
                JLabel styleLabel = new JLabel("【" + item.style + "】", SwingConstants.CENTER);
                styleLabel.setFont(new Font("標楷體", Font.BOLD, 16));
                
                // 依據不同風格設定專屬顏色
                if (item.style.equals("森之呼吸")) {
                    styleLabel.setForeground(new Color(34, 139, 34)); // 大自然綠色
                } else if (item.style.equals("霓虹核心")) {
                    styleLabel.setForeground(new Color(0, 139, 139)); // 科技感青色
                } else {
                    styleLabel.setForeground(new Color(100, 100, 100)); // 預設灰色
                }
                infoPanel.add(styleLabel);
            }
            
            JLabel priceLabel = new JLabel("價格: " + item.price, SwingConstants.CENTER);
            priceLabel.setFont(textFont);
            infoPanel.add(priceLabel);
            
            JLabel statLabel = new JLabel(item.statLabel + ": " + item.statValue, SwingConstants.CENTER);
            statLabel.setFont(textFont);
            statLabel.setForeground(new Color(0, 100, 0)); 
            infoPanel.add(statLabel);
            
            JPanel rightPanel = new JPanel(new GridBagLayout());
            rightPanel.setOpaque(false);
            rightPanel.add(infoPanel);

            add(imgLabel);    
            add(rightPanel);  
        }
    }

    // 獨立的購買確認對話框類別，負責處理購買邏輯和界面
    class PurchaseDialog extends JDialog {
        private int quantity = 1;
        private final_game game;
        public PurchaseDialog(Frame parent, ShopItem item, final_game game) {
            super(parent, "確認購買", true);

            this.game = game; // 儲存對遊戲主類別的引用
            
            setSize(400, 300);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));

            JPanel mainPanel = new JPanel(new FlowLayout());
            JLabel totalLabel = new JLabel("總價: " + (item.price * quantity));
            
            // 數量控制
            JButton btnMinus = new JButton("-");
            JTextField qtyField = new JTextField(String.valueOf(quantity), 3);
            JButton btnPlus = new JButton("+");

            // 處理數值邏輯
            ActionListener updateLogic = e -> {
                try {
                    quantity = Integer.parseInt(qtyField.getText());
                    totalLabel.setText("總價: " + (item.price * quantity));
                } catch (Exception ex) {}
            };

            btnPlus.addActionListener(e -> { quantity++; qtyField.setText(String.valueOf(quantity)); updateLogic.actionPerformed(null); });
            btnMinus.addActionListener(e -> { if(quantity > 1) quantity--; qtyField.setText(String.valueOf(quantity)); updateLogic.actionPerformed(null); });
            qtyField.addActionListener(updateLogic);

            mainPanel.add(new JLabel(item.name));
            mainPanel.add(btnMinus); mainPanel.add(qtyField); mainPanel.add(btnPlus);
            mainPanel.add(totalLabel);

            JButton btnBuy = new JButton("購買");
            // 在 PurchaseDialog 的購買按鈕監聽器中
            btnBuy.addActionListener(e -> {
                int totalCost = item.price * quantity;
                boolean isGift = item.statLabel.equals("好感度");
    
                if ((isGift ? game.giftCoins : game.homeCoins) >= totalCost) {
                    if (isGift) game.giftCoins -= totalCost;
                    else game.homeCoins -= totalCost;

                    // 【最關鍵】：直接操作傳入的 game.giftInventory
                    java.util.Map<String, Integer> target = isGift ? game.giftInventory : game.furnitureInventory;
                    int currentQty = target.getOrDefault(item.name, 0);
                    target.put(item.name, currentQty + quantity);
                
                    System.out.println("購買存入: [" + item.name + "], 目前庫存: " + target.get(item.name));
                
                    JOptionPane.showMessageDialog(this, "購買成功！");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "餘額不足！");
                }
            });

            JButton btnCancel = new JButton("取消");
            btnCancel.addActionListener(e -> dispose());

            JPanel btnPanel = new JPanel();
            btnPanel.add(btnBuy); btnPanel.add(btnCancel);

            add(mainPanel, BorderLayout.CENTER);
            add(btnPanel, BorderLayout.SOUTH);
        }
    }

    // 獨立的抽卡邏輯類別，負責根據機率產生抽卡結果，並處理保底機制
    class GachaManager {
        private int totalPullCount = 0; // 用於保底計算

        public String[] pull(int count, boolean isTenPull) {
            String[] results = new String[count];
            // 根據你設定的機率定義權重
            // N:27, R:25, SR:20, SSR:15, SSSR:10, UR:3 (總和100)
            for (int i = 0; i < count; i++) {
                totalPullCount++;
                
                // 保底機制：每30次保底SSSR
                if (totalPullCount % 30 == 0) {
                    results[i] = "光明精靈"; // SSSR
                } else {
                    results[i] = rollByProbability();
                }
            }
            return results;
        }

        private String rollByProbability() {
            int r = (int)(Math.random() * 100);
            if (r < 3) return "星辰龍";     // UR
            if (r < 13) return "光明精靈";  // SSSR
            if (r < 28) return "機械狗";    // SSR
            if (r < 48) return "魔法貓";    // SR
            if (r < 73) return Math.random() > 0.5 ? "雲朵羊" : "火焰狐"; // R
            return Math.random() > 0.66 ? "小石頭" : Math.random() > 0.33 ? "貓頭鷹" : "花鹿"; // N
        }
    }

    // 抽卡顯示區，包含滾動動畫和抽卡結果的更新
    class CardDisplayArea extends JPanel {
        private int scrollX = 0; // 當前滾動位移
        private Timer animTimer;
        private Color[] randomColors = new Color[20]; // 儲存動畫用的隨機外框顏色

        // 取得隨機的等級外框顏色
        private Color getRandomRarityColor() {
            Color[] colors = {
                new Color(150, 150, 150), // 灰
                new Color(50, 205, 50),   // 綠
                new Color(30, 144, 255),  // 藍
                new Color(138, 43, 226),  // 紫
                new Color(255, 215, 0),   // 金
                new Color(220, 20, 60)    // 紅
            };
            return colors[(int)(Math.random() * colors.length)];
        }

        public CardDisplayArea() {
            // 初始化隨機顏色
            for (int i = 0; i < randomColors.length; i++) {
                randomColors[i] = getRandomRarityColor();
            }

            // 動畫計時器
            animTimer = new Timer(30, e -> {
                scrollX += 3; // 循環滾動速度
                if (scrollX > 150) { 
                    scrollX = 0; // 當移動了一個間距，重置
                    // 將顏色往前推移，保持滾動視覺的一致性
                    for (int i = 0; i < randomColors.length - 1; i++) {
                        randomColors[i] = randomColors[i + 1];
                    }
                    randomColors[randomColors.length - 1] = getRandomRarityColor();
                }
                repaint();
            });
            animTimer.start();
        }

        public void updateCards(String[] newCards) {
            // 背景動畫僅顯示 ??，不再需要將抽出的卡牌畫在背景動畫中
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cardW = 120, cardH = 160;
            int cardY = getHeight() / 2 - cardH / 2;

            for (int i = 0; i < 20; i++) { // 繪製多個框來循環
                int currentX = scrollX + (i * (cardW + 30)) - 300;
                
                // 1. 繪製灰底
                g2d.setColor(new Color(210, 210, 210)); 
                g2d.fillRoundRect(currentX, cardY, cardW, cardH, 15, 15);
                
                // 2. 繪製隨機等級外框
                g2d.setColor(randomColors[i]); 
                g2d.setStroke(new BasicStroke(5f));
                g2d.drawRoundRect(currentX, cardY, cardW, cardH, 15, 15);

                // 3. 中間顯示 ??
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                String text = "??";
                int textX = currentX + (cardW - fm.stringWidth(text)) / 2;
                int textY = cardY + (cardH - fm.getHeight()) / 2 + fm.getAscent();
                g2d.drawString(text, textX, textY);
            }
        }
    }

    // 夥伴類別，包含名稱、圖片路徑、好感度、攻擊力和血量等屬性
    class Partner implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        String name, imgPath;
        int favor, atk, hp;
    
        public Partner(String name, String imgPath, int favor, int atk, int hp) {
            this.name = name;
            this.imgPath = imgPath;
            this.favor = favor;
            this.atk = atk;
            this.hp = hp;
        }
    }

    // 獨立的夥伴視窗類別
    class PartnerWindow extends JDialog {
        public PartnerWindow(Frame parent, List<Partner> myPartners) {
            super(parent, "我的夥伴", true);
            setSize(1100, 720);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout());

            JLabel title = new JLabel("我的夥伴", SwingConstants.CENTER);
            title.setFont(new Font("標楷體", Font.BOLD, 26));
            title.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));
            add(title, BorderLayout.NORTH);

            // 每四個一排的網格排版
            JPanel grid = new JPanel(new GridLayout(0, 4, 18, 18));
            grid.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18)); 
            grid.setBackground(new Color(240, 245, 255)); 

            for (Partner p : myPartners) {
                // 取得對應等級的外框顏色
                Color rarityColor = final_game.getRarityColor(p.name);

                // 外圓角、內八角的卡牌面板
                JPanel card = new JPanel(new BorderLayout(6, 6)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        int w = getWidth();
                        int h = getHeight();
                        int t = 10;   
                        int cut = 28; 
                        int arc = 20; 
                        
                        Polygon inner = new Polygon();
                        inner.addPoint(cut, t);             
                        inner.addPoint(w - cut, t);         
                        inner.addPoint(w - t, cut);         
                        inner.addPoint(w - t, h - cut);     
                        inner.addPoint(w - cut, h - t);     
                        inner.addPoint(cut, h - t);         
                        inner.addPoint(t, h - cut);         
                        inner.addPoint(t, cut);             
                        
                        g2d.setColor(Color.WHITE);
                        g2d.fill(inner);
                        
                        java.awt.geom.Area outerArea = new java.awt.geom.Area(
                            new java.awt.geom.RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc)
                        );
                        outerArea.subtract(new java.awt.geom.Area(inner));
                        
                        g2d.setColor(rarityColor);
                        g2d.fill(outerArea);
                        
                        g2d.dispose();
                    }
                };
                card.setOpaque(false); 
                card.setPreferredSize(new Dimension(210, 310));
                card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // 圖片
                JLabel imgLabel = new JLabel("", SwingConstants.CENTER);
                try {
                    java.awt.image.BufferedImage img =
                        javax.imageio.ImageIO.read(new java.io.File(p.imgPath));
                    imgLabel.setIcon(new ImageIcon(img.getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
                } catch (Exception ex) {
                    imgLabel.setText("?");
                    imgLabel.setFont(new Font("Serif", Font.PLAIN, 48));
                }

                // 名稱
                JLabel lblName = new JLabel(p.name, SwingConstants.CENTER);
                lblName.setFont(new Font("標楷體", Font.BOLD, 17));

                // 數值
                JPanel stats = new JPanel(new GridLayout(3, 1, 2, 2));
                stats.setOpaque(false);
                JLabel lblFavor = new JLabel("💖 好感度：" + p.favor, SwingConstants.CENTER);
                JLabel lblAtk   = new JLabel("⚔ 攻擊：" + p.atk,   SwingConstants.CENTER);
                JLabel lblHp    = new JLabel("❤ 血量：" + p.hp,    SwingConstants.CENTER);
                for (JLabel l : new JLabel[]{lblFavor, lblAtk, lblHp})
                    l.setFont(new Font("標楷體", Font.PLAIN, 14));
                stats.add(lblFavor); stats.add(lblAtk); stats.add(lblHp);

                // ==========================================
                // ★ 按鈕縮小與置中處理區塊
                // ==========================================
                JButton btnGift = new JButton("送禮物");
                btnGift.setFont(new Font("標楷體", Font.BOLD, 15));
                btnGift.setBackground(new Color(255, 180, 80));
                btnGift.setForeground(Color.WHITE);
                btnGift.setFocusPainted(false);
                btnGift.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                // 強制設定按鈕的寬度與高度 (100x35)
                btnGift.setPreferredSize(new Dimension(100, 35)); 

                final Partner fp = p;
                btnGift.addActionListener(ev -> new GiftWindow(PartnerWindow.this, fp));

                // 使用 FlowLayout 包裝按鈕，這樣它就不會被橫向拉長
                JPanel giftBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
                giftBtnPanel.setOpaque(false);
                giftBtnPanel.add(btnGift);
                // ==========================================

                JPanel bottom = new JPanel(new BorderLayout(3, 5));
                bottom.setOpaque(false);
                bottom.add(lblName,  BorderLayout.NORTH);
                bottom.add(stats,    BorderLayout.CENTER);
                // 把包裝好的面板放到底部
                bottom.add(giftBtnPanel,  BorderLayout.SOUTH);

                card.add(imgLabel, BorderLayout.CENTER);
                card.add(bottom,   BorderLayout.SOUTH);
                grid.add(card);
            }

            JScrollPane scroll = new JScrollPane(grid);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.setBorder(BorderFactory.createEmptyBorder()); 
            add(scroll, BorderLayout.CENTER);
            setVisible(true);
        }
    }

    // 送禮物專用視窗
    class GiftWindow extends JDialog {
        public GiftWindow(Dialog parent, Partner p) {
            super(parent, "送禮給 " + p.name, true);
            setSize(550, 380); // 稍微加大視窗以容納內容
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout()); 

            JPanel container = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
            final_game game = (final_game) SwingUtilities.getWindowAncestor(parent);

            String[][] gifts = {
                {"小禮物", ".\\Gift_image\\精緻小紙袋.png", "3", "All"},
                {"大禮物", ".\\Gift_image\\彩色禮物盒.png", "5", "All"},
                {game.getSpecialGiftName(p.name), ".\\Gift_image\\" + game.getSpecialGiftImg(p.name), "10", p.name}
            };

            for (String[] g : gifts) {
                JPanel box = new JPanel(new BorderLayout(0, 5));
                JButton btn = new JButton();
                File imgFile = new File(g[1]);
                if (imgFile.exists()) {
                    btn.setIcon(new ImageIcon(new ImageIcon(g[1]).getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
                } else {
                    btn.setText("?");
                }
                btn.setPreferredSize(new Dimension(100, 100));
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

                // 取得當前該項禮物的庫存
                int currentStock = game.giftInventory.getOrDefault(g[0], 0);

                btn.addActionListener(e -> {
                    // 檢查好感度是否已達上限
                    if (p.favor >= 150) {
                        JOptionPane.showMessageDialog(this, p.name + " 的好感度已經達到上限 (150)！\n不需要再送禮物囉！");
                        return; 
                    }

                    if (currentStock <= 0) {
                        JOptionPane.showMessageDialog(this, "該禮物庫存不足！快去商店購買吧！");
                        return;
                    }

                    // 呼叫自定義的選擇數量小視窗
                    showQuantityDialog(game, parent, p, g, currentStock);
                });

                // 資訊面板，包含好感度增加量與當前庫存
                JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                infoPanel.setOpaque(false);
                
                JLabel lblFavor = new JLabel("好感 +" + g[2], SwingConstants.CENTER);
                lblFavor.setFont(new Font("標楷體", Font.BOLD, 14));
                
                JLabel lblStock = new JLabel("庫存: " + currentStock, SwingConstants.CENTER);
                lblStock.setFont(new Font("標楷體", Font.PLAIN, 13));
                
                // 如果庫存為 0，字體變為紅色提醒
                if (currentStock == 0) {
                    lblStock.setForeground(new Color(200, 50, 50));
                } else {
                    lblStock.setForeground(new Color(50, 150, 50));
                }

                infoPanel.add(lblFavor);
                infoPanel.add(lblStock);

                box.add(btn, BorderLayout.CENTER);
                box.add(infoPanel, BorderLayout.SOUTH);
                container.add(box);
            }

            add(container, BorderLayout.CENTER); 
            setVisible(true);
        }

        // ==========================================
        // ★ 新增：贈送數量選擇視窗
        // ==========================================
        private void showQuantityDialog(final_game game, Dialog parentWindow, Partner p, String[] g, int maxStock) {
            JDialog qtyDialog = new JDialog(this, "選擇贈送數量", true);
            qtyDialog.setSize(380, 220);
            qtyDialog.setLocationRelativeTo(this);
            qtyDialog.setLayout(new BorderLayout(10, 10));
            qtyDialog.getContentPane().setBackground(new Color(245, 245, 255));

            int favorPerGift = Integer.parseInt(g[2]);
            
            // 智能計算：到達 150 滿好感度還差多少
            int favorNeeded = 150 - p.favor;
            // 計算需要幾顆禮物才能補滿 (無條件進位)
            int maxNeeded = (int) Math.ceil((double) favorNeeded / favorPerGift);
            // 最大允許贈送數量：在庫存與所需數量之間取最小值，避免浪費
            int maxAllowed = Math.min(maxStock, maxNeeded);

            JPanel centerPanel = new JPanel(new GridLayout(2, 1));
            centerPanel.setOpaque(false);
            centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

            JLabel titleLbl = new JLabel("贈送【" + g[0] + "】給 " + p.name, SwingConstants.CENTER);
            titleLbl.setFont(new Font("標楷體", Font.BOLD, 18));
            centerPanel.add(titleLbl);
            
            // 數量選擇 UI
            JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            ctrlPanel.setOpaque(false);
            
            JButton btnMinus = new JButton("-");
            JTextField qtyField = new JTextField("1", 3);
            qtyField.setHorizontalAlignment(JTextField.CENTER);
            qtyField.setFont(new Font("Arial", Font.BOLD, 18));
            qtyField.setEditable(false); // 禁止手動輸入防呆
            JButton btnPlus = new JButton("+");
            JButton btnMax = new JButton("最大");
            btnMax.setToolTipText("自動計算到達滿級所需數量");

            final int[] qty = {1}; // 使用陣列讓 Lambda 內部可以修改值

            Runnable updateField = () -> qtyField.setText(String.valueOf(qty[0]));

            btnMinus.addActionListener(e -> { if (qty[0] > 1) { qty[0]--; updateField.run(); } });
            btnPlus.addActionListener(e -> { if (qty[0] < maxAllowed) { qty[0]++; updateField.run(); } });
            btnMax.addActionListener(e -> { qty[0] = maxAllowed; updateField.run(); });

            ctrlPanel.add(btnMinus);
            ctrlPanel.add(qtyField);
            ctrlPanel.add(btnPlus);
            ctrlPanel.add(btnMax);
            
            centerPanel.add(ctrlPanel);
            qtyDialog.add(centerPanel, BorderLayout.CENTER);

            // 底部確認按鈕
            JPanel southPanel = new JPanel(new FlowLayout());
            southPanel.setOpaque(false);
            
            JButton btnConfirm = new JButton("確認贈送");
            btnConfirm.setBackground(new Color(60, 160, 60));
            btnConfirm.setForeground(Color.WHITE);
            btnConfirm.setFont(new Font("標楷體", Font.BOLD, 16));
            
            btnConfirm.addActionListener(e -> {
                int sendCount = qty[0];
                if (sendCount > maxStock) sendCount = maxStock;
                if (sendCount <= 0) return;

                // 扣除庫存
                game.giftInventory.put(g[0], maxStock - sendCount);

                // 計算好感度變化
                int favorBefore = p.favor;
                int gainedFavor = favorPerGift * sendCount;
                p.favor += gainedFavor;
                
                // 強制鎖定在上限 150
                if (p.favor > 150) p.favor = 150;

                // 計算等級與能力值變化
                int tierBefore = favorBefore / 15;
                int tierAfter = p.favor / 15;
                int tiers = tierAfter - tierBefore;
                
                int atkGained = tiers * 10;
                int hpGained = tiers * 20;
                
                if (tiers > 0) {
                    p.atk += atkGained;
                    p.hp  += hpGained;
                }
                
                game.saveGame();
                
                // 顯示精美的升級結算訊息
                String msg = "成功贈送 " + sendCount + " 個 " + g[0] + "！\n好感度增加了 " + (p.favor - favorBefore) + " 點！";
                if (tiers > 0) {
                    msg += "\n\n✨ 夥伴能力提升了！\nATK +" + atkGained + " / HP +" + hpGained;
                }
                if (p.favor == 150) {
                    msg += "\n💖 好感度已達最大值！";
                }
                
                JOptionPane.showMessageDialog(qtyDialog, msg);
                
                // 關閉數量視窗與送禮主視窗
                qtyDialog.dispose();
                GiftWindow.this.dispose();

                // 強制刷新後方的夥伴列表
                if (parentWindow instanceof PartnerWindow) {
                    ((PartnerWindow) parentWindow).dispose();
                    new PartnerWindow(game, game.myPartners);
                }
            });

            JButton btnCancel = new JButton("取消");
            btnCancel.setFont(new Font("標楷體", Font.BOLD, 16));
            btnCancel.addActionListener(e -> qtyDialog.dispose());

            southPanel.add(btnConfirm);
            southPanel.add(btnCancel);
            qtyDialog.add(southPanel, BorderLayout.SOUTH);

            qtyDialog.setVisible(true);
        }
    }

    // 圖鑑視窗，分為夥伴、黑化怪獸、淨化夥伴三個頁籤
    class IllustratedWindow extends JDialog {

        // 從統一配置動態生成怪獸資料（避免重複）
        private static String[][] getMonsterData() {
            String[][] monsters = new String[MONSTER_CONFIG.length][];
            for (int i = 0; i < MONSTER_CONFIG.length; i++) {
                monsters[i] = new String[]{
                    MONSTER_CONFIG[i][0],  // 怪獸名
                    MONSTER_CONFIG[i][1]   // 圖片檔名
                };
            }
            return monsters;
        }
        
        // 從統一配置動態生成淨化夥伴資料（避免重複）
        private static String[][] getPurifiedData() {
            String[][] purified = new String[MONSTER_CONFIG.length][];
            for (int i = 0; i < MONSTER_CONFIG.length; i++) {
                String partnerName = MONSTER_CONFIG[i][6]; // 淨化為(夥伴名)
                // 從 CHARACTER_FILES 取得對應檔名
                String filename = CHARACTER_FILES.getOrDefault(partnerName, partnerName + ".png");
                purified[i] = new String[]{partnerName, filename};
            }
            return purified;
        }
        
        // 對應哪隻怪獸打倒後解鎖（順序與 MONSTER_CONFIG 相同）
        private static String[] getPurifiedUnlockBy() {
            String[] unlockBy = new String[MONSTER_CONFIG.length];
            for (int i = 0; i < MONSTER_CONFIG.length; i++) {
                unlockBy[i] = MONSTER_CONFIG[i][0]; // 怪獸名
            }
            return unlockBy;
        }
        
        private static final String[][] MONSTER_DATA = getMonsterData();
        private static final String[][] PURIFIED_DATA = getPurifiedData();
        private static final String[] PURIFIED_UNLOCK_BY = getPurifiedUnlockBy();

        public IllustratedWindow(Frame parent, List<String> owned) {
            super(parent, "遊戲圖鑑", true);
            setSize(1050, 720);
            setLocationRelativeTo(parent);

            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(new Font("標楷體", Font.BOLD, 16));

            tabs.add("夥伴",     buildPartnerTab(owned));
            tabs.add("黑化怪獸", buildMonsterTab(owned));
            tabs.add("淨化夥伴", buildPurifiedTab(owned));

            add(tabs);
            setVisible(true);
        }

        // ── 夥伴頁 ──────────────────────────────────────────────
        private JScrollPane buildPartnerTab(List<String> owned) {
            JPanel p = makeGridPanel(new Color(240, 245, 255));
            // 只顯示原始 9 個角色（不包括淨化夥伴）
            for (int i = 0; i < 9; i++) {
                String[] charData = CHARACTER_CONFIG[i];
                String name = charData[0];
                boolean unlocked = owned.contains(name);
                String imgPath = ".\\Main_image\\" + charData[1];
                p.add(makeCard(name, imgPath, unlocked, false, -1, -1));
            }
            return scrollWrap(p);
        }

        // ── 黑化怪獸頁 ──────────────────────────────────────────
        private JScrollPane buildMonsterTab(List<String> owned) {
            JPanel p = makeGridPanel(new Color(255, 240, 240));
            for (String[] md : MONSTER_DATA) {
                // 需打倒過才能在圖鑑看到（以 ownedCharacters 包含怪獸名判斷）
                boolean unlocked = owned.contains(md[0]);
                String imgPath = ".\\Monster_image\\" + md[1];
                p.add(makeCard(md[0], imgPath, unlocked, false, -1, -1));
            }
            return scrollWrap(p);
        }

        // ── 淨化夥伴頁 ──────────────────────────────────────────
        private JScrollPane buildPurifiedTab(List<String> owned) {
            JPanel p = makeGridPanel(new Color(240, 255, 240));
            for (int i = 0; i < PURIFIED_DATA.length; i++) {
                String name    = PURIFIED_DATA[i][0];
                String imgFile = PURIFIED_DATA[i][1];
                boolean unlocked = owned.contains(PURIFIED_UNLOCK_BY[i]);
                String imgPath   = ".\\Main_image\\" + imgFile;
                int[] stats = final_game.getBaseStats(name);
                p.add(makeCard(name, imgPath, unlocked, true, stats[0], stats[1]));
            }
            return scrollWrap(p);
        }

        // ── 4欄 GridLayout 底板 ────────────────────────────────
        private JPanel makeGridPanel(Color bg) {
            JPanel p = new JPanel(new GridLayout(0, 4, 14, 14));
            p.setBackground(bg);
            p.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
            return p;
        }

        // ── 通用卡片製作 ────────────────────────────────────────
        private JPanel makeCard(String name, String imgPath,
                                boolean unlocked, boolean showStats,
                                int atk, int hp) {
            JPanel card = new JPanel(new BorderLayout(4, 4));
            card.setPreferredSize(new Dimension(175, showStats ? 240 : 210));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(unlocked ? new Color(100, 160, 100) : Color.GRAY, 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            card.setBackground(unlocked ? Color.WHITE : new Color(50, 50, 50));

            JLabel imgLbl = new JLabel("", SwingConstants.CENTER);
            if (unlocked) {
                File f = new File(imgPath);
                if (f.exists()) {
                    try {
                        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f);
                        imgLbl.setIcon(new ImageIcon(img.getScaledInstance(140, 140, Image.SCALE_SMOOTH)));
                    } catch (Exception ex) {
                        imgLbl.setText("？");
                        imgLbl.setFont(new Font("Serif", Font.BOLD, 48));
                        imgLbl.setForeground(Color.DARK_GRAY);
                    }
                } else {
                    imgLbl.setText("無圖");
                    imgLbl.setFont(new Font("標楷體", Font.BOLD, 20));
                    imgLbl.setForeground(Color.DARK_GRAY);
                    System.out.println("[圖鑑] 找不到圖片: " + imgPath);
                }
            } else {
                imgLbl.setText("？");
                imgLbl.setFont(new Font("Serif", Font.BOLD, 60));
                imgLbl.setForeground(new Color(80, 80, 80));
            }

            JLabel lblName = new JLabel(unlocked ? name : "???", SwingConstants.CENTER);
            lblName.setFont(new Font("標楷體", Font.BOLD, 15));
            lblName.setForeground(unlocked ? Color.BLACK : new Color(120, 120, 120));

            JPanel bottom = new JPanel(new BorderLayout(2, 3));
            bottom.setOpaque(false);
            bottom.add(lblName, BorderLayout.NORTH);

            if (showStats && unlocked) {
                JPanel statsP = new JPanel(new GridLayout(2, 1, 1, 1));
                statsP.setOpaque(false);
                JLabel lAtk = new JLabel("⚔ ATK " + atk, SwingConstants.CENTER);
                JLabel lHp  = new JLabel("❤ HP  " + hp,  SwingConstants.CENTER);
                lAtk.setFont(new Font("標楷體", Font.PLAIN, 13));
                lHp.setFont(new Font("標楷體", Font.PLAIN, 13));
                statsP.add(lAtk); statsP.add(lHp);
                bottom.add(statsP, BorderLayout.CENTER);
            }

            card.add(imgLbl, BorderLayout.CENTER);
            card.add(bottom, BorderLayout.SOUTH);
            return card;
        }

        private JScrollPane scrollWrap(JPanel p) {
            JScrollPane sp = new JScrollPane(p);
            sp.getVerticalScrollBar().setUnitIncrement(16);
            return sp;
        }
    }

    // 戰鬥視窗，包含戰鬥邏輯與UI更新
    class BattleWindow extends JDialog {
    private final String monsterName, monsterImgPath;
    private int   monsterHp, monsterMaxHp;
    private final int   monsterAtk;
    private final List<Partner> team;
    private int[] partnerCurrentHp;
    private int   turnSlot  = 0;
    private Timer battleTimer;
    private boolean battleOver = false;

    private JLabel       lblMonsterHp;
    private JProgressBar barMonster;
    private JLabel[]     lblPartnerHp, lblPartnerImg;
    private JProgressBar[] barPartner;
    private JTextArea    logArea;

    public BattleWindow(Frame parent, String monsterName, String monsterImgPath,
                        int monsterHp, int monsterAtk, List<Partner> team) {
        super(parent, "⚔ 戰鬥中：" + monsterName, true);
        this.monsterName    = monsterName;
        this.monsterImgPath = monsterImgPath;
        this.monsterHp      = monsterHp;
        this.monsterMaxHp   = monsterHp;
        this.monsterAtk     = monsterAtk;
        this.team           = team;
        partnerCurrentHp    = new int[team.size()];
        for (int i = 0; i < team.size(); i++) partnerCurrentHp[i] = team.get(i).hp;

        setSize(960, 680);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(new Color(25, 25, 45));
        setLayout(new BorderLayout(8, 8));
        buildUI();
        // 必須在 setVisible 之前用 invokeLater 排入 startBattle，
        // 否則 modal dialog 的 setVisible 會阻塞，startBattle 永遠不執行。
        SwingUtilities.invokeLater(() -> startBattle());
        setVisible(true);
    }

    private void buildUI() {
        // ─ 怪獸區（上方）
        JPanel monsterPanel = new JPanel(new BorderLayout(12, 5));
        monsterPanel.setBackground(new Color(55, 18, 18));
        monsterPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel lblMonsterImg = new JLabel("", SwingConstants.CENTER);
        try {
            java.awt.image.BufferedImage img =
                javax.imageio.ImageIO.read(new java.io.File(monsterImgPath));
            lblMonsterImg.setIcon(new ImageIcon(img.getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
        } catch (Exception ex) {
            lblMonsterImg.setText("🐉");
            lblMonsterImg.setFont(new Font("Serif", Font.PLAIN, 60));
        }
        lblMonsterImg.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        barMonster = new JProgressBar(0, monsterMaxHp);
        barMonster.setValue(monsterHp);
        barMonster.setStringPainted(true);
        barMonster.setString(monsterHp + " / " + monsterMaxHp);
        barMonster.setForeground(new Color(220, 50, 50));
        barMonster.setBackground(new Color(80, 20, 20));
        barMonster.setPreferredSize(new Dimension(0, 28));

        lblMonsterHp = new JLabel(
            monsterName + "  HP: " + monsterHp + " / " + monsterMaxHp + "  ATK: " + monsterAtk);
        lblMonsterHp.setForeground(Color.WHITE);
        lblMonsterHp.setFont(new Font("標楷體", Font.BOLD, 17));

        JPanel mRight = new JPanel(new BorderLayout(4, 6));
        mRight.setOpaque(false);
        mRight.add(lblMonsterHp, BorderLayout.NORTH);
        mRight.add(barMonster,   BorderLayout.CENTER);

        monsterPanel.add(lblMonsterImg, BorderLayout.WEST);
        monsterPanel.add(mRight,        BorderLayout.CENTER);

        // ─ 夥伴區（下方）
        JPanel teamPanel = new JPanel(new GridLayout(1, Math.max(team.size(), 1), 15, 0));
        teamPanel.setBackground(new Color(18, 35, 55));
        teamPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 12, 20));

        lblPartnerHp  = new JLabel[team.size()];
        lblPartnerImg = new JLabel[team.size()];
        barPartner    = new JProgressBar[team.size()];

        for (int i = 0; i < team.size(); i++) {
            Partner p = team.get(i);
            JPanel card = new JPanel(new BorderLayout(5, 5));
            card.setBackground(new Color(28, 55, 85));
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 140, 200), 2),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

            lblPartnerImg[i] = new JLabel("", SwingConstants.CENTER);
            try {
                java.awt.image.BufferedImage img =
                    javax.imageio.ImageIO.read(new java.io.File(p.imgPath));
                lblPartnerImg[i].setIcon(
                    new ImageIcon(img.getScaledInstance(90, 90, Image.SCALE_SMOOTH)));
            } catch (Exception ex) { lblPartnerImg[i].setText("?"); }

            barPartner[i] = new JProgressBar(0, p.hp);
            barPartner[i].setValue(p.hp);
            barPartner[i].setStringPainted(true);
            barPartner[i].setString(p.hp + " / " + p.hp);
            barPartner[i].setForeground(new Color(50, 200, 100));
            barPartner[i].setBackground(new Color(18, 55, 28));
            barPartner[i].setPreferredSize(new Dimension(0, 22));

            lblPartnerHp[i] = new JLabel(
                String.format("<html><center><b>%s</b><br>ATK %d</center></html>",
                    p.name, p.atk), SwingConstants.CENTER);
            lblPartnerHp[i].setForeground(Color.WHITE);
            lblPartnerHp[i].setFont(new Font("標楷體", Font.PLAIN, 14));

            JPanel pBottom = new JPanel(new BorderLayout(2, 3));
            pBottom.setOpaque(false);
            pBottom.add(lblPartnerHp[i], BorderLayout.NORTH);
            pBottom.add(barPartner[i],   BorderLayout.CENTER);

            card.add(lblPartnerImg[i], BorderLayout.CENTER);
            card.add(pBottom,          BorderLayout.SOUTH);
            teamPanel.add(card);
        }

        // ─ 戰鬥日誌（中間）
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(18, 18, 32));
        logArea.setForeground(new Color(215, 215, 170));
        logArea.setFont(new Font("標楷體", Font.PLAIN, 15));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 160)),
            "戰鬥紀錄", 0, 0,
            new Font("標楷體", Font.BOLD, 14), Color.LIGHT_GRAY));

        // ─ 快速結算按鈕
        JButton btnSkip = new JButton("⚡ 快速結算");
        btnSkip.setFont(new Font("標楷體", Font.BOLD, 18));
        btnSkip.setBackground(new Color(160, 100, 30));
        btnSkip.setForeground(Color.WHITE);
        btnSkip.setFocusPainted(false);
        btnSkip.addActionListener(e -> skipBattle());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(new Color(25, 25, 45));
        btnPanel.add(btnSkip);

        JPanel southWrap = new JPanel(new BorderLayout());
        southWrap.setBackground(new Color(25, 25, 45));
        southWrap.add(teamPanel, BorderLayout.CENTER);
        southWrap.add(btnPanel,  BorderLayout.SOUTH);

        add(monsterPanel, BorderLayout.NORTH);
        add(logScroll,    BorderLayout.CENTER);
        add(southWrap,    BorderLayout.SOUTH);
    }

    private void startBattle() {
        appendLog("⚔ 戰鬥開始！" + monsterName + " 出現了！");
        StringBuilder sb = new StringBuilder("  出戰夥伴：");
        for (Partner p : team) sb.append(p.name).append("(ATK ").append(p.atk).append(") ");
        appendLog(sb.toString());
        appendLog("  " + monsterName + "：HP " + monsterMaxHp + "  ATK " + monsterAtk);
        appendLog("─────────────────────────────");
        battleTimer = new Timer(3000, e -> doTurn());
        battleTimer.setInitialDelay(1500);
        battleTimer.start();
    }

    private void doTurn() {
        if (battleOver) return;

        List<Integer> alive = new ArrayList<>();
        for (int i = 0; i < team.size(); i++)
            if (partnerCurrentHp[i] > 0) alive.add(i);

        if (monsterHp <= 0)  { endBattle(true);  return; }
        if (alive.isEmpty()) { endBattle(false); return; }

        // 順序：夥伴0 → 夥伴1 → 怪獸 → 夥伴0 → …
        int slot = turnSlot % (team.size() + 1);

        if (slot < team.size()) {
            if (partnerCurrentHp[slot] <= 0) { turnSlot++; doTurn(); return; }
            int dmg = team.get(slot).atk;
            monsterHp = Math.max(0, monsterHp - dmg);
            appendLog(String.format("【%s】攻擊 %s，造成 %d 傷害！%s 剩餘 HP %d",
                team.get(slot).name, monsterName, dmg, monsterName, monsterHp));
            updateMonsterUI();
        } else {
            // 怪獸攻擊血量最高的存活夥伴
            int target = alive.stream()
                .max(java.util.Comparator.comparingInt(i -> partnerCurrentHp[i]))
                .orElse(alive.get(0));
            int dmg = monsterAtk;
            partnerCurrentHp[target] = Math.max(0, partnerCurrentHp[target] - dmg);
            appendLog(String.format("%s 攻擊【%s】，造成 %d 傷害！%s 剩餘 HP %d",
                monsterName, team.get(target).name, dmg,
                team.get(target).name, partnerCurrentHp[target]));
            updatePartnerUI(target);
        }
        turnSlot++;

        boolean allDead = true;
        for (int hp : partnerCurrentHp) if (hp > 0) { allDead = false; break; }
        if      (monsterHp <= 0) endBattle(true);
        else if (allDead)        endBattle(false);
    }

    private void skipBattle() {
        if (battleOver) return;
        battleTimer.stop();
        appendLog("\n⚡ 快速結算中...");
        int teamAtk = 0, teamHp = 0;
        for (int i = 0; i < team.size(); i++) {
            if (partnerCurrentHp[i] > 0) {
                teamAtk += team.get(i).atk;
                teamHp  += partnerCurrentHp[i];
            }
        }
        // 誰先把對方打死誰贏
        double rtkMonster = teamAtk    > 0 ? (double) monsterHp / teamAtk    : Double.MAX_VALUE;
        double rtkTeam    = monsterAtk > 0 ? (double) teamHp    / monsterAtk : Double.MAX_VALUE;
        endBattle(rtkMonster <= rtkTeam);
    }

    private void endBattle(boolean win) {
        if (battleOver) return;
        battleOver = true;
        if (battleTimer != null) battleTimer.stop();
        SwingUtilities.invokeLater(() -> {
            if (win) {
                appendLog("\n🎉 勝利！" + monsterName + " 被淨化了！");
                // 解鎖圖鑑：將打倒的怪獸加入 ownedCharacters
                if (!ownedCharacters.contains(monsterName)) {
                    ownedCharacters.add(monsterName);
                }
                // 查找對應的淨化夥伴，加入 myPartners
                String purifiedName = final_game.getPurifiedPartnerName(monsterName);
                if (purifiedName != null) {
                    String purifiedImg = ".\\Main_image\\" + getCharacterFileName(purifiedName);
                    boolean alreadyOwned = myPartners.stream()
                        .anyMatch(p -> p.name.equals(purifiedName));
                    if (!alreadyOwned) {
                        int[] stats = final_game.getBaseStats(purifiedName);
                        myPartners.add(new Partner(purifiedName, purifiedImg, 0, stats[0], stats[1]));
                    }
                }
                currentEnergy = Math.max(0, currentEnergy - 20); // 勝利消耗 20 能量
                saveGame(); // 勝利後立即存檔
                JOptionPane.showMessageDialog(this,
                    "🎉 勝利！\n" + monsterName + " 已被淨化！\n夥伴們辛苦了！\n📖 圖鑑已更新！\n⚡ 消耗 20 能量（剩餘：" + currentEnergy + "/" + maxEnergy + "）",
                    "勝利", JOptionPane.INFORMATION_MESSAGE);
            } else {
                appendLog("\n💀 敗北...夥伴們全部倒下了。");
                JOptionPane.showMessageDialog(this,
                    "💀 敗北...\n所有夥伴都倒下了，下次再挑戰吧！",
                    "敗北", JOptionPane.WARNING_MESSAGE);
            }
            dispose();
        });
    }

    private void updateMonsterUI() {
        SwingUtilities.invokeLater(() -> {
            barMonster.setValue(monsterHp);
            barMonster.setString(monsterHp + " / " + monsterMaxHp);
            lblMonsterHp.setText(
                monsterName + "  HP: " + monsterHp + " / " + monsterMaxHp + "  ATK: " + monsterAtk);
        });
    }

    private void updatePartnerUI(int idx) {
        SwingUtilities.invokeLater(() -> {
            barPartner[idx].setValue(partnerCurrentHp[idx]);
            barPartner[idx].setString(partnerCurrentHp[idx] + " / " + team.get(idx).hp);
            if (partnerCurrentHp[idx] <= 0) {
                barPartner[idx].setForeground(Color.GRAY);
                lblPartnerImg[idx].setEnabled(false);
                lblPartnerHp[idx].setForeground(new Color(200, 80, 80));
                lblPartnerHp[idx].setText(String.format(
                    "<html><center><b>%s</b><br><font color='red'>陣亡</font></center></html>",
                    team.get(idx).name));
            }
        });
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}

    // 夥伴選擇視窗，戰鬥前選擇出戰夥伴與觀看故事說明
    class PartnerSelectWindow extends JDialog {
        public PartnerSelectWindow(Frame parent, String monsterName, String monsterImg,
                                   int monsterHp, int monsterAtk, String storyPath) {
            super(parent, "選擇出戰夥伴", true);
            setLayout(new BorderLayout());
            setSize(700, 480);
            setLocationRelativeTo(parent);

            JLabel title = new JLabel("選擇最多 3 位夥伴出戰", SwingConstants.CENTER);
            title.setFont(new Font("標楷體", Font.BOLD, 22));
            title.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));
            add(title, BorderLayout.NORTH);

            List<Partner> available = myPartners;
            List<Partner> selected = new ArrayList<>();

            JPanel listPanel = new JPanel(new GridLayout(0, 1, 5, 5));
            listPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
            JScrollPane scroll = new JScrollPane(listPanel);
            add(scroll, BorderLayout.CENTER);

            if (available.isEmpty()) {
                listPanel.add(new JLabel("你還沒有任何夥伴，請先抽卡！", SwingConstants.CENTER));
            } else {
                for (Partner p : available) {
                    JPanel row = new JPanel(new BorderLayout(10, 5));
                    row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));

                    JLabel imgLbl = new JLabel();
                    try {
                        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.File(p.imgPath));
                        imgLbl.setIcon(new ImageIcon(img.getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
                    } catch (Exception ex) { imgLbl.setText("?"); }

                    JLabel info = new JLabel(String.format(
                        "<html><b>%s</b>　好感度: %d　ATK: %d　HP: %d</html>",
                        p.name, p.favor, p.atk, p.hp));
                    info.setFont(new Font("標楷體", Font.PLAIN, 15));

                    JToggleButton chk = new JToggleButton("選擇");
                    chk.setFont(new Font("標楷體", Font.BOLD, 14));
                    chk.addActionListener(ev -> {
                        if (chk.isSelected()) {
                            if (selected.size() >= 3) {
                                chk.setSelected(false);
                                JOptionPane.showMessageDialog(this, "最多只能選 3 位夥伴！");
                            } else {
                                selected.add(p);
                                chk.setText("已選");
                                row.setBackground(new Color(200, 240, 200));
                            }
                        } else {
                            selected.remove(p);
                            chk.setText("選擇");
                            row.setBackground(null);
                        }
                    });

                    row.add(imgLbl, BorderLayout.WEST);
                    row.add(info,   BorderLayout.CENTER);
                    row.add(chk,    BorderLayout.EAST);
                    listPanel.add(row);
                }
            }

            // ★ 修改點：按鈕改為「出戰！」，並先打開故事說明，然後再打開戰鬥視窗
            JButton btnGo = new JButton("⚔ 出戰！");
            btnGo.setFont(new Font("標楷體", Font.BOLD, 18));
            btnGo.setBackground(new Color(200, 60, 60));
            btnGo.setForeground(Color.WHITE);
            btnGo.setFocusPainted(false);
            btnGo.addActionListener(e -> {
                if (selected.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "請至少選擇一位夥伴！");
                    return;
                }
                if (currentEnergy < 20) {
                    JOptionPane.showMessageDialog(this,
                        "⚡ 能量不足！\n進入戰鬥需要 20 能量。\n目前能量：" + currentEnergy + " / " + maxEnergy,
                        "能量不足", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                dispose();
                new StoryAnimation(parent, monsterName, monsterImg, monsterHp, monsterAtk, storyPath, new ArrayList<>(selected));
            });

            JPanel south = new JPanel();
            south.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            south.add(btnGo);
            add(south, BorderLayout.SOUTH);

            setVisible(true);
        }
    }

    // 故事動畫視窗，展示怪獸故事與戰鬥前的氛圍營造
    class StoryAnimation extends JDialog {
        private int currentLineIndex = 0;
        private List<String> storyLines = new ArrayList<>();
        private JLabel textLabel;
        private JButton btnNext;
        private JPanel animationPanel;
        
        private String monsterName, monsterImg, storyPath;
        private int monsterHp, monsterAtk;
        private List<Partner> team;
        
        // ★ 新增這兩行：將顏色定義為類別層級的屬性
        private Color bgColor;
        private Color fgColor;

        public StoryAnimation(Frame parent, String monsterName, String monsterImg,
                              int monsterHp, int monsterAtk, String storyPath, List<Partner> team) {
            super(parent, "故事說明", true);
            this.monsterName = monsterName;
            this.monsterImg = monsterImg;
            this.monsterHp = monsterHp;
            this.monsterAtk = monsterAtk;
            this.storyPath = storyPath;
            this.team = team;

            // ── 獲取玩家設定 ──
            final_game game = (final_game) parent;
            String sizeSel = game.imagePanel.textSizeStr;
            String colorSel = game.imagePanel.textBoxColorStr;

            int fontSize = 18;
            if ("小".equals(sizeSel)) fontSize = 14;
            else if ("大".equals(sizeSel)) fontSize = 24;

            // ★ 修改：直接賦值給類別屬性，不再使用區域變數
            this.bgColor = Color.WHITE;
            this.fgColor = Color.BLACK;
            switch (colorSel) {
                case "黑色(白字)": this.bgColor = new Color(40, 40, 40); this.fgColor = Color.WHITE; break;
                case "淡藍色(黑字)": this.bgColor = new Color(200, 230, 255); this.fgColor = Color.BLACK; break;
                case "淡粉色(黑字)": this.bgColor = new Color(255, 210, 220); this.fgColor = Color.BLACK; break;
                case "白色(黑字)": default: this.bgColor = Color.WHITE; this.fgColor = Color.BLACK; break;
            }

            setLayout(new BorderLayout());
            setSize(800, 500);
            setLocationRelativeTo(parent);
            
            loadStoryLines();

            // ── 繪製圓角陰影文字框 ──
            JPanel container = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(0, 0, 0, 60));
                    g2d.fillRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 20, 20);
                    // ★ 現在這裡存取的是類別屬性，完全合法
                    g2d.setColor(bgColor); 
                    g2d.fillRoundRect(0, 0, getWidth() - 16, getHeight() - 16, 20, 20);
                    g2d.dispose();
                }
            };
            container.setOpaque(false);
            container.setBorder(BorderFactory.createEmptyBorder(25, 25, 35, 35));

            textLabel = new JLabel("", SwingConstants.CENTER);
            textLabel.setFont(new Font("標楷體", Font.BOLD, fontSize));
            textLabel.setForeground(this.fgColor); // 使用屬性
            container.add(textLabel, BorderLayout.CENTER);
            ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // 為視窗與文字框之間增加 15 像素的間距
            add(container, BorderLayout.CENTER);

            // ── 過場與按鈕 ──
            animationPanel = new JPanel();
            animationPanel.setVisible(false);
            getLayeredPane().add(animationPanel, JLayeredPane.PALETTE_LAYER);
            animationPanel.setBounds(0, 0, 700, 500);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSkip = new JButton("跳過");
            btnNext = new JButton("NEXT");
            btnSkip.addActionListener(e -> { dispose(); new BattleWindow(parent, monsterName, monsterImg, monsterHp, monsterAtk, team); });
            btnNext.addActionListener(e -> {
                if (currentLineIndex < storyLines.size() - 1) { currentLineIndex++; updateText(); }
                else { dispose(); new BattleWindow(parent, monsterName, monsterImg, monsterHp, monsterAtk, team); }
            });
            btnPanel.add(btnSkip); btnPanel.add(btnNext);
            add(btnPanel, BorderLayout.SOUTH);

            updateText();
            setVisible(true);
        }

        private void loadStoryLines() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(storyPath), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) storyLines.add(line.trim());
                }
                String bossLine = "【BOSS：" + monsterName + "（黑化）" + 
                                  (monsterName.contains("鏽蝕海怪") ? "「被污染海洋吞噬的水之精靈」】" : "「失去森林的守護神」】");
                storyLines.add(bossLine);
            } catch (IOException e) { storyLines.add("故事檔案讀取錯誤。"); }
        }

        private void updateText() {
            String line = storyLines.get(currentLineIndex);
            if (line.contains("（畫面全黑 2 秒）")) triggerAnimation(Color.BLACK);
            else if (line.contains("（畫面全白 2 秒）")) triggerAnimation(Color.WHITE);
            else {
                textLabel.setText("<html><center>" + line + "</center></html>");
                if (currentLineIndex == storyLines.size() - 1) {
                    btnNext.setText("出戰！");
                    btnNext.setBackground(new Color(200, 60, 60));
                    btnNext.setForeground(Color.WHITE);
                }
            }
        }

        private void triggerAnimation(Color color) {
            textLabel.setVisible(false);
            animationPanel.setBackground(color);
            animationPanel.setVisible(true);
            Timer timer = new Timer(2000, e -> {
                animationPanel.setVisible(false);
                textLabel.setVisible(true);
                if (currentLineIndex < storyLines.size() - 1) {
                    currentLineIndex++;
                    updateText();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }
    
    // 森之工作坊 (WorkGameWindow)
    class WorkGameWindow extends JDialog {
        private final int GRID_SIZE = 4;
        private JButton[][] gridButtons = new JButton[GRID_SIZE][GRID_SIZE];
        private int[][] cellStates = new int[GRID_SIZE][GRID_SIZE]; 
        private int[][] cellLifes = new int[GRID_SIZE][GRID_SIZE];  
        private int sessionCard = 0, sessionGift = 0, sessionHome = 0;
        private JLabel lblStatus;
        private JToggleButton btnAuto;
        private Timer gameTimer;
        private final_game mainGame;

        public WorkGameWindow(final_game parent) {
            super(parent, "森之工作坊", true);
            this.mainGame = parent;
            setSize(600, 600);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout());

            JPanel topPanel = new JPanel(new GridLayout(2, 1));
            topPanel.setBackground(new Color(240, 248, 240));
            lblStatus = new JLabel("本次工作回報：🪙 卡幣 +0 | 🎁 禮幣 +0 | 🏠 家幣 +0", SwingConstants.CENTER);
            topPanel.add(new JLabel("點擊收集成熟的森林資源！", SwingConstants.CENTER));
            topPanel.add(lblStatus);
            add(topPanel, BorderLayout.NORTH);

            JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 10, 10));
            gridPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    JButton btn = new JButton("🍃");
                    final int row = r, col = c;
                    btn.addActionListener(e -> harvestCell(row, col));
                    gridButtons[r][c] = btn;
                    gridPanel.add(btn);
                }
            }
            add(gridPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel();
            btnAuto = new JToggleButton("自動模式 OFF");
            btnAuto.addActionListener(e -> btnAuto.setText(btnAuto.isSelected() ? "自動模式 ON" : "自動模式 OFF"));
            JButton btnExit = new JButton("結束工作");
            btnExit.addActionListener(e -> endWork());
            bottomPanel.add(btnAuto); bottomPanel.add(btnExit);
            add(bottomPanel, BorderLayout.SOUTH);

            // 1. Timer 改快
            gameTimer = new Timer(300, e -> { // 300ms 循環一次
                // 2. 資源生成機率稍微調高 (0.45 -> 0.6)
                if (Math.random() < 0.6) {
                    int r = (int)(Math.random()*GRID_SIZE), c = (int)(Math.random()*GRID_SIZE);
                    if (cellStates[r][c] == 0) { 
                        cellStates[r][c] = (int)(Math.random()*3)+1; 
                        cellLifes[r][c] = 6; // 增加生命週期，避免消失太快
                        updateCellUI(r, c); 
                    }
                }
    
                // 3. 自動模式全域採收
                if (btnAuto.isSelected()) {
                    for (int r = 0; r < GRID_SIZE; r++) 
                        for (int c = 0; c < GRID_SIZE; c++) 
                            if (cellStates[r][c] != 0) harvestCell(r, c);
                }
            });

            gameTimer.start();
            setVisible(true);
        }

        private void clearCell(int r, int c) { cellStates[r][c] = 0; gridButtons[r][c].setText("🍃"); gridButtons[r][c].setBackground(null); }
        private void updateCellUI(int r, int c) {
            String[] labels = {"", "🪙 卡幣", "🎁 禮物", "🏠 原木"};
            gridButtons[r][c].setText(labels[cellStates[r][c]]);
        }
        private void harvestCell(int r, int c) {
            int type = cellStates[r][c]; 
            if (type == 0) return;
    
            // 設定加成倍率：如果開了自動，可以給予 1.5 倍或 2 倍的獎勵
            int reward = btnAuto.isSelected() ? 4 : 2; 

            if (type == 1) { sessionCard += reward; mainGame.cardCoins += reward; }
            else if (type == 2) { sessionGift += reward; mainGame.giftCoins += reward; }
            else { sessionHome += reward; mainGame.homeCoins += reward; }
    
            clearCell(r, c);
            lblStatus.setText("本次工作回報：🪙 卡幣 +" + sessionCard + " | 🎁 禮幣 +" + sessionGift + " | 🏠 家幣 +" + sessionHome);
            mainGame.repaint();
        }
        private void endWork() { gameTimer.stop(); mainGame.saveGame(); dispose(); }
    }
} //結束