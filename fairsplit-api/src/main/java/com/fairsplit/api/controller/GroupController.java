package com.fairsplit.api.controller;

import com.fairsplit.api.dto.AddMemberRequest;
import com.fairsplit.api.dto.CreateGroupRequest;
import com.fairsplit.api.service.GroupService;
import com.fairsplit.core.entity.Group;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    private final UserRepository userRepository;

    public GroupController(GroupService groupService, UserRepository userRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
    }


    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody CreateGroupRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group newGroup = groupService.createGroup(request.name(), user.getId());
        return ResponseEntity.ok(newGroup);
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<Void> addMember(@PathVariable UUID groupId,
                          @RequestBody AddMemberRequest request) {
        groupService.addMember(groupId, request.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Group>> getGroupsForUser(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Group> groups = groupService.getGroupsForUser(user.getId());
        return ResponseEntity.ok(groups);
    }
}
