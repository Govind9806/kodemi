package com.example.course_service.config;

import com.example.course_service.model.CategoryEntity;
import com.example.course_service.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CategoryDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryDataSeeder.class);

    // Category group constants - avoids duplicated string literals (Sonar S1192)
    private static final String CATEGORY_IT_SOFTWARE = "IT & Software";
    private static final String CATEGORY_FINANCE = "Finance & Accounting";
    private static final String CATEGORY_MARKETING = "Marketing";
    private static final String CATEGORY_OFFICE_PRODUCTIVITY = "Office Productivity";
    private static final String CATEGORY_DESIGN = "Design";
    private static final String CATEGORY_PERSONAL_DEV = "Personal Development";
    private static final String CATEGORY_TEACHING = "Teaching & Academics";
    private static final String CATEGORY_BUSINESS = "Business";
    private static final String CATEGORY_HEALTH_FITNESS = "Health & Fitness";

    private final CategoryRepository categoryRepository;

    // Shared counter so displayOrder stays sequential across all seed methods,
    // without using ++ inside a method-call subexpression (Sonar S881).
    private final AtomicInteger displayOrderCounter = new AtomicInteger(1);

    public CategoryDataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        try {
            List<CategoryEntity> existingCategories = categoryRepository.findAll();

            if (existingCategories != null && !existingCategories.isEmpty()) {
                log.info("Categories already exist. Skipping default category seeding. Existing count: {}",
                        existingCategories.size());
                return;
            }

            log.info("No categories found. Inserting fixed default categories...");
            seedCategories();
            log.info("Default categories inserted successfully!");
        } catch (Exception e) {
            log.error("Failed to seed categories due to database/DynamoDB error", e);
        }
    }

    private void seedCategories() {
        seedCategoriesPart1();
        seedCategoriesPart2();
        seedCategoriesPart3();
        seedCategoriesPart4();
        seedCategoriesPart5();
    }

    private void seedCategoriesPart1() {
        save("IT_CERT", "IT Certifications", CATEGORY_IT_SOFTWARE, "it-certifications",
                "AWS AI Practitioner", "AWS Cloud Practitioner", "CCNA", "CompTIA A+", "Security+");

        save("FIN_COMP", "Compliance & Audit", CATEGORY_FINANCE, "compliance-audit",
                "Fraud Analytics", "Internal Controls", "Risk Management");

        save("MKT_CONTENT", "Content Marketing", CATEGORY_MARKETING, "content-marketing",
                "Blogging", "Content Strategy", "Copywriting", "Storytelling");

        save("IT_SWDEV", "Software Development", CATEGORY_IT_SOFTWARE, "software-development",
                "Algorithms", "C", "C#", "C++", "Data Structures", "Go", "Java", "OOP", "PHP", "Python", "System Design");

        save("OFF_PM", "Project Management Tools", CATEGORY_OFFICE_PRODUCTIVITY, "project-management-tools",
                "Asana", "Jira", "MS Project", "Trello");

        save("DES_GRAPHIC", "Graphic Design", CATEGORY_DESIGN, "graphic-design",
                "Branding", "Canva", "Illustrator", "Photoshop");

        save("PD_LEADER", "Leadership Skills", CATEGORY_PERSONAL_DEV, "leadership-skills",
                "Decision Making", "Influence", "Leadership");

        save("TE_HUM", "Humanities", CATEGORY_TEACHING, "humanities",
                "History", "Literature", "Philosophy");

        save("BUS_HR", "Human Resources", CATEGORY_BUSINESS, "human-resources",
                "Employee Engagement", "HR Analytics", "Payroll", "Performance Management", "Recruitment");

        save("HLT_HOLISTIC", "Holistic Health", CATEGORY_HEALTH_FITNESS, "holistic-health",
                "Alternative Medicine", "Natural Healing", "Wellness");

        save("BUS_MGMT", "Management & Leadership", CATEGORY_BUSINESS, "management-leadership",
                "Decision Making", "ISO 9001", "Leadership", "Management Skills", "Quality Management",
                "Strategic Management", "Team Management");

        save("FIN_CORP", "Corporate Finance", CATEGORY_FINANCE, "corporate-finance",
                "Capital Budgeting", "Corporate Valuation", "Financial Management", "Financial Planning");

        save("OFF_MS", "Microsoft Productivity", CATEGORY_OFFICE_PRODUCTIVITY, "microsoft-productivity",
                "Advanced Excel", "Excel", "Power BI", "PowerPoint", "VBA");

        save("DES_WEB", "Web Design", CATEGORY_DESIGN, "web-design",
                "Adobe XD", "CSS", "HTML", "Responsive Design", "Wireframing");

        save("HLT_YOGA", "Yoga", CATEGORY_HEALTH_FITNESS, "yoga",
                "Breathing Techniques", "Flexibility", "Hatha Yoga", "Yoga Basics");
    }

    private void seedCategoriesPart2() {
        save("TE_SCI", "Science", CATEGORY_TEACHING, "science",
                "Biology", "Chemistry", "Physics");

        save("PD_REL", "Relationships", CATEGORY_PERSONAL_DEV, "relationships",
                "Conflict Resolution", "Healthy Relationships");

        save("FIN_ACC", "Accounting", CATEGORY_FINANCE, "accounting",
                "Accounting", "Bookkeeping", "QuickBooks", "TallyPrime", "Xero");

        save("TE_TRAIN", "Teacher Training", CATEGORY_TEACHING, "teacher-training",
                "Curriculum Design", "Teaching Methods");

        save("OFF_SAP", "SAP", CATEGORY_OFFICE_PRODUCTIVITY, "sap",
                "SAP FICO", "SAP MM", "SAP SD");

        save("BUS_ECOM", "E-Commerce", CATEGORY_BUSINESS, "e-commerce",
                "Amazon FBA", "Dropshipping", "E-Commerce Fundamentals", "Online Selling");

        save("BUS_OPS", "Operations & Supply Chain", CATEGORY_BUSINESS, "operations-supply-chain",
                "Lean", "Logistics", "Operations Management", "Six Sigma", "Supply Chain");

        save("DES_FASHION", "Fashion Design", CATEGORY_DESIGN, "fashion-design",
                "Garment Design", "Pattern Making", "Textile Design");

        save("HLT_DANCE", "Dance", CATEGORY_HEALTH_FITNESS, "dance",
                "Contemporary", "Dance Fitness", "Hip Hop", "Zumba");

        save("MKT_BRAND", "Branding", CATEGORY_MARKETING, "branding",
                "Brand Identity", "Brand Strategy", "Positioning");

        save("IT_IOT", "Embedded Systems & IoT", CATEGORY_IT_SOFTWARE, "embedded-iot",
                "Arduino", "ESP32", "Embedded C", "HMI", "Industrial IoT", "PCB Design", "PLC", "Raspberry Pi");

        save("HLT_NUTRITION", "Nutrition & Diet", CATEGORY_HEALTH_FITNESS, "nutrition-diet",
                "Dieting", "Gut Health", "Sports Nutrition", "Weight Loss");

        save("FIN_CERT", "Finance Certifications", CATEGORY_FINANCE, "finance-certifications",
                "CFA", "CPA", "FRM");

        save("PD_COMM", "Communication Skills", CATEGORY_PERSONAL_DEV, "communication-skills",
                "Presentation Skills", "Public Speaking");

        save("IT_SUPPORT", "IT Support", CATEGORY_IT_SOFTWARE, "it-support",
                "Active Directory", "Linux Administration", "PowerShell", "Troubleshooting", "Windows Server");
    }

    private void seedCategoriesPart3() {
        save("MKT_SEO", "SEO", CATEGORY_MARKETING, "seo",
                "Keyword Research", "Link Building", "Local SEO", "SEO");

        save("MKT_SOCIAL", "Social Media Marketing", CATEGORY_MARKETING, "social-media-marketing",
                "Facebook", "Instagram", "LinkedIn", "TikTok", "YouTube Marketing");

        save("FIN_ANALYSIS", "Financial Analysis", CATEGORY_FINANCE, "financial-analysis",
                "Excel for Finance", "Financial Analysis", "Forecasting", "Ratio Analysis");

        save("TE_MATH", "Mathematics", CATEGORY_TEACHING, "mathematics",
                "Algebra", "Calculus", "Probability", "Statistics");

        save("IT_CYBER", "Cyber Security", CATEGORY_IT_SOFTWARE, "cyber-security",
                "Ethical Hacking", "Information Security", "OSINT", "Penetration Testing", "SOC Analyst", "Security+");

        save("PD_PROD", "Productivity", CATEGORY_PERSONAL_DEV, "productivity",
                "Notion", "Obsidian", "Speed Reading", "Time Management");

        save("DES_TOOLS", "Design Tools", CATEGORY_DESIGN, "design-tools",
                "After Effects", "Canva", "Figma", "Illustrator", "Photoshop");

        save("BUS_RE", "Real Estate", CATEGORY_BUSINESS, "real-estate",
                "Airbnb Hosting", "Property Management", "Real Estate Investing");

        save("HLT_MEDITATION", "Meditation", CATEGORY_HEALTH_FITNESS, "meditation",
                "Guided Meditation", "Mindfulness", "Relaxation");

        save("FIN_BANK", "Banking", CATEGORY_FINANCE, "banking",
                "Investment Banking", "Retail Banking");

        save("OFF_ORACLE", "Oracle", CATEGORY_OFFICE_PRODUCTIVITY, "oracle",
                "Oracle ERP", "Oracle SQL");

        save("IT_MOBILE", "Mobile Development", CATEGORY_IT_SOFTWARE, "mobile-development",
                "Android", "Flutter", "Java", "Kotlin", "React Native", "Swift", "iOS");

        save("MKT_AFFILIATE", "Affiliate Marketing", CATEGORY_MARKETING, "affiliate-marketing",
                "Affiliate Programs", "CPA Marketing", "Passive Income");

        save("MKT_DIGITAL", "Digital Marketing", CATEGORY_MARKETING, "digital-marketing",
                "ChatGPT Marketing", "Internet Marketing", "Marketing Strategy", "Sales Funnel");

        save("MKT_ADS", "Advertising", CATEGORY_MARKETING, "advertising",
                "Facebook Ads", "Google Ads", "Media Buying", "PPC");
    }

    private void seedCategoriesPart4() {
        save("DES_INTERIOR", "Interior Design", CATEGORY_DESIGN, "interior-design",
                "AutoCAD", "Home Design", "SketchUp", "Space Planning");

        save("DES_GAME", "Game Design", CATEGORY_DESIGN, "game-design",
                "Game Mechanics", "Level Design", "Unity", "Unreal Engine");

        save("TE_ENG", "Engineering", CATEGORY_TEACHING, "engineering",
                "Automotive", "Civil", "Electrical", "Electronics", "Mechanical");

        save("FIN_INV", "Investing & Trading", CATEGORY_FINANCE, "investing-trading",
                "Day Trading", "Investing", "Portfolio Management", "Stock Trading");

        save("BUS_COMM", "Communication Skills", CATEGORY_BUSINESS, "communication-skills",
                "Business Communication", "Presentation Skills", "Public Speaking", "Storytelling");

        save("PD_CAREER", "Career Development", CATEGORY_PERSONAL_DEV, "career-development",
                "Interview Preparation", "Job Search", "LinkedIn Optimization", "Resume Writing");

        save("IT_QA", "Testing & QA", CATEGORY_IT_SOFTWARE, "testing-qa",
                "API Testing", "Automation Testing", "JMeter", "Manual Testing", "Postman", "Selenium");

        save("FIN_TAX", "Taxation", CATEGORY_FINANCE, "taxation",
                "GST", "Tax Preparation", "Transfer Pricing", "VAT");

        save("PD_HAPPY", "Happiness & Wellbeing", CATEGORY_PERSONAL_DEV, "happiness-wellbeing",
                "Gratitude", "Positive Psychology");

        save("PD_SPIRIT", "Spirituality", CATEGORY_PERSONAL_DEV, "spirituality",
                "Meditation", "Self Awareness");

        save("IT_NET", "Networking", CATEGORY_IT_SOFTWARE, "networking",
                "CCNA", "CCNP", "CompTIA Network+", "Network Fundamentals", "Routing", "Switching");

        save("IT_CLOUD", "Cloud Computing", CATEGORY_IT_SOFTWARE, "cloud-computing",
                "AWS", "Azure", "Cloud Architecture", "Docker", "GCP", "Kubernetes", "Terraform");

        save("OFF_AI", "AI Productivity Tools", CATEGORY_OFFICE_PRODUCTIVITY, "ai-productivity-tools",
                "ChatGPT", "Notion", "Prompt Engineering", "ServiceNow");

        save("IT_WEB", "Web Development", CATEGORY_IT_SOFTWARE, "web-development",
                "Angular", "CSS", "Django", "Express", "FastAPI", "HTML", "JavaScript", "Laravel",
                "MongoDB", "Next.js", "Node.js", "React", "SQL", "Spring Boot", "Vue");

        save("DES_3D", "3D & Animation", CATEGORY_DESIGN, "3d-animation",
                "3D Modeling", "After Effects", "Blender", "Motion Graphics", "Unreal Engine");
    }

    private void seedCategoriesPart5() {
        save("IT_AI", "Data Science & AI", CATEGORY_IT_SOFTWARE, "data-science-ai",
                "ChatGPT", "Data Analysis", "Deep Learning", "Generative AI", "Machine Learning",
                "Power BI", "Prompt Engineering", "Python", "Tableau");

        save("TE_ONLINE", "Online Education", CATEGORY_TEACHING, "online-education",
                "EdTech", "LMS", "Online Teaching");

        save("BUS_ENT", "Entrepreneurship", CATEGORY_BUSINESS, "entrepreneurship",
                "Business Development", "Business Fundamentals", "Business Planning", "Business Strategy",
                "Entrepreneurship", "Innovation", "Startup", "Startup Funding");

        save("BUS_SALES", "Sales & Customer Success", CATEGORY_BUSINESS, "sales-customer-success",
                "B2B Sales", "CRM", "Cold Calling", "Customer Experience", "Customer Service", "Negotiation", "Sales Skills");

        save("IT_DEVOPS", "DevOps", CATEGORY_IT_SOFTWARE, "devops",
                "CI/CD", "Docker", "Git", "GitHub", "Jenkins", "Kubernetes", "Linux", "Terraform");

        save("MKT_PRODUCT", "Product Marketing", CATEGORY_MARKETING, "product-marketing",
                "Customer Research", "Go-To-Market", "Product Launch");

        save("HLT_MENTAL", "Mental Wellness", CATEGORY_HEALTH_FITNESS, "mental-wellness",
                "CBT", "Counseling", "Psychology", "Stress Management");

        save("PD_GROWTH", "Personal Growth", CATEGORY_PERSONAL_DEV, "personal-growth",
                "Emotional Intelligence", "Life Coaching", "Mindfulness", "NLP");

        save("DES_UIUX", "UI/UX Design", CATEGORY_DESIGN, "ui-ux-design",
                "Design Thinking", "Figma", "Product Design", "UI Design", "UX Design");

        save("TE_LANG", "Languages", CATEGORY_TEACHING, "languages",
                "English", "French", "German", "Japanese", "Spanish");

        save("OFF_GOOGLE", "Google Productivity", CATEGORY_OFFICE_PRODUCTIVITY, "google-productivity",
                "Apps Script", "Google Sheets", "Looker Studio", "Workspace");

        save("HLT_FITNESS", "Fitness Training", CATEGORY_HEALTH_FITNESS, "fitness-training",
                "Home Workout", "Muscle Building", "Pilates", "Strength Training");

        save("TE_SOC", "Social Sciences", CATEGORY_TEACHING, "social-sciences",
                "Economics", "Psychology", "Sociology");

        save("HLT_SPORTS", "Sports", CATEGORY_HEALTH_FITNESS, "sports",
                "Athletic Performance", "Recovery", "Sports Coaching");
    }

    private void save(
            String id,
            String cardTitle,
            String category,
            String slug,
            String... keywords
    ) {
        CategoryEntity categoryEntity = new CategoryEntity();

        categoryEntity.setCategoryId(id);
        categoryEntity.setName(cardTitle);
        categoryEntity.setSlug(slug);

        // Stores main category: IT & Software, Business, Marketing, etc.
        categoryEntity.setDescription(category);

        categoryEntity.setDisplayOrder(displayOrderCounter.getAndIncrement());
        categoryEntity.setIsActive(true);

        // Stores keywords under cardTitle.
        Map<String, List<String>> subCategories = Map.of(cardTitle, List.of(keywords));
        categoryEntity.setSubCategories(subCategories);

        categoryRepository.save(categoryEntity);
    }
}