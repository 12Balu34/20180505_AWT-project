package com.polimi.awt.controller;

import com.polimi.awt.exception.PreconditionFailedException;
import com.polimi.awt.exception.UnauthorizedException;
import com.polimi.awt.model.Campaign;
import com.polimi.awt.model.CampaignStatistics;
import com.polimi.awt.model.CampaignStatus;
import com.polimi.awt.model.users.Manager;
import com.polimi.awt.model.users.User;
import com.polimi.awt.model.users.Worker;
import com.polimi.awt.payload.CampaignRequest;
import com.polimi.awt.payload.HttpResponseStatus.ApiResponse;
import com.polimi.awt.payload.HttpResponseStatus.OkResponse;
import com.polimi.awt.repository.AnnotationRepository;
import com.polimi.awt.repository.CampaignRepository;
import com.polimi.awt.repository.PeakRepository;
import com.polimi.awt.repository.UserRepository;
import com.polimi.awt.security.CurrentUser;
import com.polimi.awt.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.polimi.awt.model.RoleName.MANAGER;

@RestController
@RequestMapping("/api")
public class CampaignController {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private PeakRepository peakRepository;


    @GetMapping("/campaigns")
    public List<Campaign> getCampaigns(@CurrentUser UserPrincipal currentUser) {
        User user = userRepository.findUserById(currentUser.getId());
        //Return all owned campaigns if user is a Manager
        if (user.rolesContainsRoleName(MANAGER)) {
            return campaignRepository.findAllByManager_Id(user.getId());
        }
        /*//Return all campaigns with status STARTED if user is a Worker
        return campaignRepository.findCampaignByCampaignStatus(CampaignStatus.STARTED);*/

        return campaignRepository.findEnrolledCampaigns(currentUser.getId());
    }

    @GetMapping("/campaigns/{campaignId}")
    public Campaign getCampaignById(@PathVariable Long campaignId, @CurrentUser UserPrincipal currentUser) {

        return campaignRepository.findCampaignById(campaignId);
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasAuthority('MANAGER')")
    public Campaign createCampaign(@RequestBody CampaignRequest request, @CurrentUser UserPrincipal currentUser) {
        Manager manager = (Manager) userRepository.findUserById(currentUser.getId());
        return campaignRepository.save(manager.createCampaign(request.getName()));
    }

    @PatchMapping("/campaigns/{campaignId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ApiResponse updateCampaignStatus(@CurrentUser UserPrincipal currentUser, @PathVariable Long campaignId) {

        Manager manager = (Manager) userRepository.findUserById(currentUser.getId());
        Campaign campaign = campaignRepository.findCampaignById(campaignId);

        if (!campaign.getManager().equals(manager)) {
            throw  new PreconditionFailedException("You are not authorized to change the status of this campaign.");
        }

        campaignRepository.save(manager.updateCampaignStatus(campaign));
        return new OkResponse("Updated status to " + campaign.getCampaignStatus());
    }

    @PostMapping("/campaigns/{campaignId}")
    @PreAuthorize("hasAuthority('WORKER')")
    public ApiResponse enrollInCampaign(@CurrentUser UserPrincipal currentUser, @PathVariable Long campaignId) {

        Campaign campaign = campaignRepository.findCampaignById(campaignId);
        if (!campaign.getCampaignStatus().equals(CampaignStatus.STARTED)) {
            throw new PreconditionFailedException("You cannot enroll in this campaign.");
        }
        Worker worker = (Worker) userRepository.findUserById(currentUser.getId());
        campaignRepository.save(worker.enrollInCampaign(campaign));
        return new OkResponse("Enrolled successfully in Campaign " + campaign.getName());
    }

    @GetMapping("campaigns/{campaignId}/statistics")
    @PreAuthorize("hasAuthority('MANAGER')")
    public CampaignStatistics getCampaignStatistics (@CurrentUser UserPrincipal currentUser, @PathVariable Long campaignId) {

        Campaign campaign = campaignRepository.findCampaignById(campaignId);
        if (!campaign.getManager().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException();
        }

        int totalNumberOfPeaks = peakRepository.countPeaksByCampaignId(campaignId);
        int numberOfAnnotatedPeaks = annotationRepository.countPeaksInAnnotatedState(campaignId);
        int numberOfStartedPeaks = totalNumberOfPeaks - numberOfAnnotatedPeaks;

        return new CampaignStatistics(numberOfStartedPeaks, numberOfAnnotatedPeaks,
                annotationRepository.countPeaksWithRejectedAnnotations(campaignId),
                annotationRepository.countNumberOfConflictsByCampaignId(campaignId));
    }

}
