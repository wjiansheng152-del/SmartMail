package com.smartmail.contact.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.contact.entity.Contact;
import com.smartmail.contact.service.ContactService;
import com.smartmail.contact.web.dto.ContactCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理接口：对客户（联系人）的增删查与分页查询，支持按分组过滤。
 * <p>
 * 所有接口均在租户上下文中执行，数据按当前租户 Schema 隔离。
 * </p>
 */
@RestController
@RequestMapping("/api/contact/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * 新建客户：邮箱必填且唯一，姓名、手机可选。
     *
     * @param request 含 email、name、mobile
     * @return 200 + Result&lt;Contact&gt; 含入库后的客户（含 id、createTime、updateTime）
     */
    @PostMapping
    public Result<Contact> create(@Valid @RequestBody ContactCreateRequest request) {
        Contact contact = new Contact();
        contact.setEmail(request.getEmail());
        contact.setName(request.getName());
        contact.setMobile(request.getMobile());
        contact.setCreateTime(java.time.LocalDateTime.now());
        contact.setUpdateTime(java.time.LocalDateTime.now());
        contactService.save(contact);
        return Result.ok(contact);
    }

    /**
     * 根据 ID 查询单个客户，不存在时返回 404。
     */
    @GetMapping("/{id}")
    public Result<Contact> get(@PathVariable Long id) {
        Contact contact = contactService.getById(id);
        if (contact == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        return Result.ok(contact);
    }

    /**
     * 分页查询客户；若传 groupId 则仅返回该分组内的客户。
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @param groupId 可选，分组 ID，不传则查全部
     */
    @GetMapping("/page")
    public Result<IPage<Contact>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long groupId) {
        return Result.ok(contactService.pageList(page, size, groupId));
    }

    /**
     * 按 ID 删除客户，不存在也返回 200（幂等）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contactService.removeById(id);
        return Result.ok();
    }
}
