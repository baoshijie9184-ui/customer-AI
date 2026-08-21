package com.richard.fyoung.customeradmin.aiconfig.skill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgentSkill;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentSkillMapper;
import com.richard.fyoung.customeradmin.aiconfig.skill.dto.SkillFileVO;
import com.richard.fyoung.customeradmin.aiconfig.skill.dto.SkillSaveRequest;
import com.richard.fyoung.customeradmin.aiconfig.skill.dto.SkillUploadFile;
import com.richard.fyoung.customeradmin.aiconfig.skill.dto.SkillUploadParseResult;
import com.richard.fyoung.customeradmin.aiconfig.skill.dto.SkillVO;
import com.richard.fyoung.customeradmin.aiconfig.skill.entity.AiSkill;
import com.richard.fyoung.customeradmin.aiconfig.skill.entity.AiSkillFile;
import com.richard.fyoung.customeradmin.aiconfig.skill.mapper.AiSkillFileMapper;
import com.richard.fyoung.customeradmin.aiconfig.skill.mapper.AiSkillMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.runtime.AgentInstanceCache;
import com.richard.fyoung.customerwork.data.skill.storage.SkillContentPublisher;
import com.richard.fyoung.customerwork.data.skill.storage.SkillFileContent;
import com.richard.fyoung.customerwork.data.skill.storage.SkillStorageTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Skill 管理。
 *
 * <p>zip 上传的技能包 = SKILL.md 本体 + references/scripts 等附属文件：SKILL.md 存
 * {@code ai_skill.content}，附属文件全量存 {@code ai_skill_file}（文本/二进制统一按字节），
 * 保存时随事务落库并发布到勾选的存储目标；运行时由 AdminAgentInstanceFactory 把两者一起
 * 落盘交给 FileSystemSkillRepository 加载，技能里引用的脚本/文档才真正生效。</p>
 *
 * <p>新建/编辑时除入库外，把 SKILL.md 正文与附属文件发布到用户勾选的存储目标（local/nacos/sftp）。
 * 发布走 {@link SkillContentPublisher} SPI，发布失败让事务回滚，保证"保存成功=目标已上传"；
 * 取消勾选/删除时对相应目标做尽力而为的清理。智能体运行时消费仍从数据库读，不经这些目标。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SkillService {

    public abstract PageResult<SkillVO> page(PageQuery query);

    public abstract SkillVO get(Long id);

    public abstract void create(SkillSaveRequest request);

    public abstract void update(Long id, SkillSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 解析上传文件为技能包：{@code .md} 直接整篇当 SKILL.md 正文（无附属文件）；{@code .zip} 以
     * 最浅层 SKILL.md（大小写不敏感）所在目录为技能根，正文取该 SKILL.md，根目录下其余全部文件
     * （含子目录、二进制）按相对路径收进附属文件列表。不落库——解析结果回填前端表单，
     * 仍走既有的 create/update 接口随事务保存。
     */
    public abstract SkillUploadParseResult parseUploadContent(MultipartFile file);
}
