package com.fairsplit.api.service;

import com.fairsplit.core.entity.Group;

import com.fairsplit.core.entity.GroupMember;
import com.fairsplit.core.entity.User;
import com.fairsplit.core.repository.GroupRepository;
import com.fairsplit.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.fairsplit.core.entity.GroupMember.Role.MEMBER;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public Group createGroup(String name, UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Group newGroup = Group.builder()
                .name(name)
                .createdBy(creator)
                .build();

        GroupMember newGroupMember = GroupMember.builder()
                .group(newGroup)
                .user(creator)
                .role(GroupMember.Role.ADMIN)
                .build();

        newGroup.getMembers().add(newGroupMember);
        return groupRepository.save(newGroup);
    }

    public void addMember(UUID groupId, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GroupMember groupMember = GroupMember.builder()
                .group(group)
                .user(user)
                .role(MEMBER)
                .build();

        group.getMembers().add(groupMember);
        groupRepository.save(group);
    }

    public List<Group> getGroupsForUser(UUID userId) {
        return groupRepository.findByMembers_User_Id(userId);
    }
}