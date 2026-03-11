package com.smartmail.contact.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartmail.common.exception.BizException;
import com.smartmail.common.exception.ErrorCode;
import com.smartmail.common.result.Result;
import com.smartmail.contact.entity.Contact;
import com.smartmail.contact.service.ContactService;
import com.smartmail.common.tenant.TenantContext;
import com.smartmail.contact.web.dto.ContactCreateRequest;
import com.smartmail.contact.web.dto.ContactListItem;
import com.smartmail.contact.web.dto.ContactResponse;
import com.smartmail.contact.web.dto.ContactUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

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
     * 新建客户：邮箱必填且唯一，姓名、手机可选。id 为租户内连续序号（删除后可复用）。
     *
     * @param request 含 email、name、mobile
     * @return 200 + Result&lt;ContactResponse&gt; 含 id（local_id）、createTime、updateTime
     */
    @PostMapping
    public Result<ContactResponse> create(@Valid @RequestBody ContactCreateRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        Contact contact = new Contact();
        contact.setTenantId(tenantId);
        contact.setLocalId(contactService.nextLocalIdForTenant(tenantId));
        contact.setEmail(request.getEmail());
        contact.setName(request.getName());
        contact.setMobile(request.getMobile());
        contact.setCreateTime(java.time.LocalDateTime.now());
        contact.setUpdateTime(java.time.LocalDateTime.now());
        contactService.save(contact);
        return Result.ok(ContactResponse.from(contact));
    }

    /**
     * 根据 id（租户内序号 local_id）查询单个客户，不存在时返回 404。
     */
    @GetMapping("/{id}")
    public Result<ContactResponse> get(@PathVariable Integer id) {
        String tenantId = TenantContext.getTenantId();
        Contact contact = contactService.getByLocalIdAndTenant(id, tenantId != null ? tenantId : "default");
        if (contact == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        return Result.ok(ContactResponse.from(contact));
    }

    /**
     * 根据 id（租户内序号）更新客户信息；不存在返回 404，邮箱与其他客户重复返回 409。
     */
    @PutMapping("/{id}")
    public Result<ContactResponse> update(@PathVariable Integer id, @Valid @RequestBody ContactUpdateRequest request) {
        Contact contact = contactService.updateByLocalId(id, request);
        return Result.ok(ContactResponse.from(contact));
    }

    /**
     * 分页查询客户；每条 id 为租户内序号（连续、可复用）。若传 groupId 则仅返回该分组内的客户。
     */
    @GetMapping("/page")
    public Result<IPage<ContactListItem>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long groupId) {
        IPage<Contact> raw = contactService.pageList(page, size, groupId);
        long offset = (raw.getCurrent() - 1) * raw.getSize();
        List<ContactListItem> records = IntStream.range(0, raw.getRecords().size())
                .mapToObj(i -> {
                    Contact c = raw.getRecords().get(i);
                    ContactListItem item = new ContactListItem();
                    BeanUtils.copyProperties(c, item);
                    item.setId(c.getLocalId() != null ? c.getLocalId().longValue() : null);
                    item.setSequence((int) offset + i + 1);
                    return item;
                })
                .toList();
        Page<ContactListItem> resultPage = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        resultPage.setRecords(records);
        return Result.ok(resultPage);
    }

    /**
     * 按 id（租户内序号）删除客户，不存在也返回 200（幂等）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        String tenantId = TenantContext.getTenantId();
        contactService.removeByLocalIdAndTenant(id, tenantId != null ? tenantId : "default");
        return Result.ok();
    }
}
