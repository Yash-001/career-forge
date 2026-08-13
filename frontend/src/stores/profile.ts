import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  profileApi,
  type ProfileData,
  type WorkExperience,
  type Education,
  type Skill,
  type UpdateProfilePayload,
  type WorkExperiencePayload,
  type EducationPayload,
  type SkillPayload,
} from '@/api/profile'

export const useProfileStore = defineStore('profile', () => {
  const profile = ref<ProfileData | null>(null)
  const experiences = ref<WorkExperience[]>([])
  const educations = ref<Education[]>([])
  const skills = ref<Skill[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function loadAll() {
    loading.value = true
    error.value = null
    try {
      const [p, e, ed, s] = await Promise.all([
        profileApi.getProfile().catch(() => null),
        profileApi.getExperiences(),
        profileApi.getEducations(),
        profileApi.getSkills(),
      ])
      profile.value = p
      experiences.value = e
      educations.value = ed
      skills.value = s
    } catch (err) {
      error.value = profileApi.extractError(err).message
    } finally {
      loading.value = false
    }
  }

  async function saveProfile(payload: UpdateProfilePayload): Promise<ProfileData> {
    const updated = await profileApi.upsertProfile(payload)
    profile.value = updated
    return updated
  }

  async function addExperience(payload: WorkExperiencePayload): Promise<WorkExperience> {
    const created = await profileApi.createExperience(payload)
    experiences.value.push(created)
    return created
  }

  async function updateExperience(id: string, payload: WorkExperiencePayload): Promise<WorkExperience> {
    const updated = await profileApi.updateExperience(id, payload)
    const idx = experiences.value.findIndex((e) => e.id === id)
    if (idx !== -1) experiences.value[idx] = updated
    return updated
  }

  async function removeExperience(id: string): Promise<void> {
    await profileApi.deleteExperience(id)
    experiences.value = experiences.value.filter((e) => e.id !== id)
  }

  async function addEducation(payload: EducationPayload): Promise<Education> {
    const created = await profileApi.createEducation(payload)
    educations.value.push(created)
    return created
  }

  async function updateEducation(id: string, payload: EducationPayload): Promise<Education> {
    const updated = await profileApi.updateEducation(id, payload)
    const idx = educations.value.findIndex((e) => e.id === id)
    if (idx !== -1) educations.value[idx] = updated
    return updated
  }

  async function removeEducation(id: string): Promise<void> {
    await profileApi.deleteEducation(id)
    educations.value = educations.value.filter((e) => e.id !== id)
  }

  async function addSkill(payload: SkillPayload): Promise<Skill> {
    const created = await profileApi.createSkill(payload)
    skills.value.push(created)
    return created
  }

  async function updateSkill(id: string, payload: SkillPayload): Promise<Skill> {
    const updated = await profileApi.updateSkill(id, payload)
    const idx = skills.value.findIndex((s) => s.id === id)
    if (idx !== -1) skills.value[idx] = updated
    return updated
  }

  async function removeSkill(id: string): Promise<void> {
    await profileApi.deleteSkill(id)
    skills.value = skills.value.filter((s) => s.id !== id)
  }

  return {
    profile,
    experiences,
    educations,
    skills,
    loading,
    error,
    loadAll,
    saveProfile,
    addExperience,
    updateExperience,
    removeExperience,
    addEducation,
    updateEducation,
    removeEducation,
    addSkill,
    updateSkill,
    removeSkill,
  }
})
